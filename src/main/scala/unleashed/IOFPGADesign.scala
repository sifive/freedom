// See LICENSE for license details.

package sifive.freedom.unleashed

import Chisel._
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util.{ElaborationArtefacts,ResetCatchAndSync}

import sifive.blocks.devices.msi._
import sifive.blocks.devices.chiplink._

import sifive.fpgashells.shell._
import sifive.fpgashells.clocks._

import sifive.freedom.unleashed.u500vc707devkit.FreedomUVC707Config

//-------------------------------------------------------------------------
// ShadowRAMHack -- shadow 512MiB of DDR at 0x6000_0000 from 0x30_0000_000
//                  this makes it possible to boot linux using FPGA DDR
//-------------------------------------------------------------------------

class ShadowRAMHack(implicit p: Parameters) extends LazyModule
{
  val from = AddressSet(0x60000000L, 0x1fffffffL)
  val to = AddressSet(0x3000000000L, 0x1fffffffL)

  val node = TLAdapterNode(
    clientFn  = {cp => cp },
    managerFn = { mp =>
      require (mp.managers.size == 1)
      mp.copy(managers = mp.managers.map { m =>
        m.copy(address = m.address ++ Seq(from))
      })
    })

  lazy val module = new LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, _), (out, _)) =>
      out <> in
      out.a.bits.address := Mux(
        from.contains(in.a.bits.address),
        in.a.bits.address + UInt(to.base - from.base),
        in.a.bits.address)
    }
  }
}

//-------------------------------------------------------------------------
// IOFPGAChip
//-------------------------------------------------------------------------

case object IOFPGAFrequencyKey extends Field[Double](100.0)
class IOFPGADesign()(implicit p: Parameters) extends LazyModule
{
  val chiplinkparams = ChipLinkParams(
        TLUH = AddressSet.misaligned(0,             0x40000000L),                   // Aloe MMIO              [  0GB, 1GB)
        TLC =  AddressSet.misaligned(0x60000000L,   0x20000000L) ++                 // local memory behind L2 [1.5GB, 2GB)
               AddressSet.misaligned(0x80000000L,   0x2000000000L - 0x80000000L) ++ // Aloe DDR               [  2GB, 128GB)
               AddressSet.misaligned(0x3000000000L, 0x1000000000L),                 // local memory behind L2 [192GB, 256GB)
        syncTX = true
  )
  val localRoute = AddressSet.misaligned(0x40000000L, 0x20000000L) ++               // local MMIO             [  1GB, 1.5GB)
                   AddressSet.misaligned(0x2000000000L, 0x1000000000L)              // local MMIO             [128GB, 192GB)

  // Core clocking
  val sysClock  = p(ClockInputOverlayKey).head(ClockInputOverlayParams())
  val sysTap    = ClockIdentityNode()
  val corePLL   = p(PLLFactoryKey)()
  val coreGroup = ClockGroup()
  val wrangler  = LazyModule(new ResetWrangler)
  val coreClock = ClockSinkNode(freqMHz = p(IOFPGAFrequencyKey))
  coreClock := wrangler.node := coreGroup := corePLL := sysTap := sysClock

  // SoC components
  val sbar = LazyModule(new TLXbar)
  val xbar = LazyModule(new TLXbar)
  val mbar = LazyModule(new TLXbar)
  val serr = LazyModule(new TLError(ErrorParams(Seq(AddressSet(0x2800000000L, 0xffffffffL)), 8, 128, true), beatBytes = 8))
  val msimaster = LazyModule(new MSIMaster(Seq(MSITarget(address=0x2020000, spacing=4, number=10))))
  val sram = LazyModule(new TLRAM(AddressSet(0x2400000000L, 0xfff), beatBytes = 8))
  // We only support the first DDR or PCIe controller in this design
  val ddr = p(DDROverlayKey).headOption.map(_(DDROverlayParams(0x3000000000L, wrangler.node)))
  val (pcie, pcieInt) = p(PCIeOverlayKey).headOption.map(_(PCIeOverlayParams(wrangler.node))).unzip
  // We require ChipLink, though, obviously
  val link = p(ChipLinkOverlayKey).head(ChipLinkOverlayParams(
    params   = chiplinkparams,
    txGroup  = coreGroup,
    txData   = coreClock,
    wrangler = wrangler.node))

  private def filter(m: TLManagerParameters) = // keep only managers that are locally routed
    if (m.address.exists(a => localRoute.exists(_.overlaps(a)))) Some(m) else None

  // local master Xbar
  mbar.node := msimaster.masterNode
  pcie.foreach { mbar.node := TLFIFOFixer() := _ }

  // split local master traffic either to local routing or off-chip
  link := TLBuffer() := mbar.node
  xbar.node := TLFilter(filter) := TLBuffer() := mbar.node
  xbar.node := TLBuffer() := link

  // receive traffic either from local routing or from off-chip
  sbar.node := TLBuffer() := TLAtomicAutomata() := TLFIFOFixer() := TLHintHandler() := TLBuffer() := TLWidthWidget(4) := xbar.node

  // local slave Xbar
  sram.node := TLFragmenter(8, 64) := sbar.node
  serr.node := sbar.node
  ddr.foreach {
    val hack = LazyModule(new ShadowRAMHack)
    _ := hack.node := sbar.node
  }
  pcie.foreach { _ :*= TLWidthWidget(8) :*= sbar.node }

  // interrupts are fed into chiplink via MSI
  pcieInt.foreach { msimaster.intNode := _ }

  // grab LEDs if any
  val leds = p(LEDOverlayKey).headOption.map(_(LEDOverlayParams()))

  lazy val module = new LazyRawModuleImp(this) {
    val (core, _) = coreClock.in(0)
    childClock := core.clock
    childReset := core.reset

    val (sys, sysEdge) = sysTap.out(0)
    withClockAndReset(sys.clock, sys.reset) {
      // Blink LEDs to indicate clock good
      val hz = UInt((sysEdge.clock.freqMHz * 1000000.0).toLong)
      val divider = RegInit(hz)
      val oneSecond = divider === UInt(0)
      divider := Mux(oneSecond, hz, divider - UInt(1))

      val toggle = RegInit(UInt(2))
      when (oneSecond) { toggle := ~toggle }

      // Connect status indications
      leds.foreach { _ := Cat(wrangler.module.status, childReset, toggle) }
    }
  }
}

// Allow frequency of the design to be controlled by the Makefile
class WithFrequency(MHz: Double) extends Config((site, here, up) => {
  case IOFPGAFrequencyKey => MHz
})

class With100MHz extends WithFrequency(100)
class With125MHz extends WithFrequency(125)
class With150MHz extends WithFrequency(150)
class With200MHz extends WithFrequency(200)

class IOFPGAConfig extends Config(
  new FreedomUVC707Config().alter((site, here, up) => {
    case DesignKey => { (p:Parameters) => new IOFPGADesign()(p) }
  }))
