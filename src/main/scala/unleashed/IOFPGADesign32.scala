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

import nvidia.blocks.dla._

import sifive.fpgashells.shell._
import sifive.fpgashells.clocks._

//-------------------------------------------------------------------------
// IOFPGAChip
//-------------------------------------------------------------------------

case object IOFPGAFrequencyKey extends Field[Double](100.0)
class IOFPGADesign32()(implicit p: Parameters) extends LazyModule with BindingScope
{
  // Merge the expansion board's DTS with the Aloe DTS
  Device.skipIndexes(10000)
  lazy val dts = DTS(bindingTree)
  lazy val prelude = io.Source.fromFile("bootrom/U540Config.dts").mkString
  lazy val dtb = DTB(prelude + dts.substring(10))
  ElaborationArtefacts.add("dts", dts)
  ElaborationArtefacts.add("graphml", graphML)

  val chiplinkparams = ChipLinkParams(
        TLUH = AddressSet.misaligned(0,             0x40000000L),                   // U540 MMIO              [  0GB, 1GB)
        TLC =  AddressSet.misaligned(0xc0000000L,   0x40000000L) ++                 // local memory behind L2 [ , )
               AddressSet.misaligned(0x80000000L,   0x100000) ++                    // E740 main memory       [  , )
        syncTX = true
  )
  val localRoute = AddressSet.misaligned(0x40000000L, 0x40000000L) ++              // local MMIO              [ 1GB , 2GB )

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
  val serr = LazyModule(new TLError(DevNullParams(Seq(AddressSet(0x2f00000000L, 0x7fffffffL)), 8, 128, region = RegionType.TRACKED), beatBytes = 8))
  val dtbrom = LazyModule(new TLROM(0x2ff0000000L, 0x10000, dtb.contents, executable = false, beatBytes = 8))
  val msimaster = LazyModule(new MSIMaster(Seq(MSITarget(address=0x2020000, spacing=4, number=10))))
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

  // Tap traffic for progress LEDs
  val iTap = TLIdentityNode()
  val oTap = TLIdentityNode()

  // split local master traffic either to local routing or off-chip
  xbar.node := TLFilter(filter) := TLBuffer() := mbar.node

  (link
    := TLBuffer(BufferParams.none, BufferParams.default)
    := oTap
    := TLBuffer(BufferParams.default, BufferParams.none)
    := mbar.node)

  (xbar.node
    := TLBuffer(BufferParams.none, BufferParams.default)
    := iTap
    := TLBuffer(BufferParams.default, BufferParams.none)
    := link)

  // receive traffic either from local routing or from off-chip
  sbar.node := TLBuffer() := TLAtomicAutomata() := TLBuffer() := TLFIFOFixer() := TLHintHandler() := TLBuffer() := TLWidthWidget(4) := xbar.node

  // local slave Xbar
  serr.node := sbar.node
  dtbrom.node := TLFragmenter(8, 64) := sbar.node
  val sram = LazyModule(new TLRAM(AddressSet(0x2f90000000L, 0xfff), beatBytes = 8))
  sram.node := TLFragmenter(8, 64) := sbar.node  

  // grab LEDs if any
  val leds = p(LEDOverlayKey).headOption.map(_(LEDOverlayParams()))

  // Bind IOFPGA interrupts to PLIC on the HiFive unleashed
  val plicDevice = new Device {
    def describe(resources: ResourceBindings) = Description("plic", Map())
    override val label = "{/soc/interrupt-controller@c000000}"
  }
  ResourceBinding {
    val sources = msimaster.intNode.edges.in.map(_.source)
    val flatSources = (sources zip sources.map(_.num).scanLeft(0)(_+_).init).flatMap {
      case (s, o) => s.sources.map(z => z.copy(range = z.range.offset(o)))}
    flatSources.foreach { s => s.resources.foreach { r =>
      (s.range.start until s.range.end).foreach { i => r.bind(plicDevice, ResourceInt(i+32)) }}}
  }

  // Bind IOFPGA soc nodes to HiFive unleashed
  ResourceBinding {
    Resource(ResourceAnchors.root, "compat").bind(ResourceString("iofpga"))
    Resource(ResourceAnchors.root, "model").bind(ResourceString("iofpga-dev"))
    Resource(ResourceAnchors.root, "width").bind(ResourceInt(2))
    Resource(ResourceAnchors.soc,  "width").bind(ResourceInt(2))
    val managers = ManagerUnification(sbar.node.edges.in.head.manager.managers)
    managers.foreach { manager => manager.resources.foreach { _.bind(manager.toResource) }}
  }

  lazy val module = new LazyRawModuleImp(this) {
    println(dts)

    val (core, _) = coreClock.in(0)
    childClock := core.clock
    childReset := core.reset

    // Count all messages
    val xferWidth = 24 // 16MT
    def count(edge: TLEdge, x: DecoupledIO[TLChannel]): UInt = {
      val reg = RegInit(UInt(0, width=xferWidth))
      when (x.fire() && edge.first(x)) { reg := reg + UInt(1) }
      reg
    }
    def count(edge: TLEdge, x: TLBundle): UInt = {
      count(edge, x.a) +
      count(edge, x.b) +
      count(edge, x.c) +
      count(edge, x.d) +
      count(edge, x.e)
    }
    def count(x: (TLBundle, TLEdge)): UInt = count(x._2, x._1)

    val traffic = withClockAndReset(core.clock, core.reset) {
      RegNext(count(iTap.out(0)) + count(oTap.out(0)))
    }

    val (sys, sysEdge) = sysTap.out(0)
    val toggle = withClockAndReset(sys.clock, sys.reset) {
      // Blink LEDs to indicate clock good
      val hz = UInt((sysEdge.clock.freqMHz * 1000000.0).toLong)
      val divider = RegInit(hz)
      val oneSecond = divider === UInt(0)
      divider := Mux(oneSecond, hz, divider - UInt(1))
      val toggle = RegInit(UInt(2))
      when (oneSecond) { toggle := ~toggle }
      toggle
    }

    // Connect status indication
    leds.foreach { leds =>
      val width = leds.getWidth
      val stride = xferWidth / width
      val trafficLED = Cat(Seq.tabulate(width) { i => traffic(i*stride) }.reverse)
      val resetLED = Cat(wrangler.module.status, toggle)
      leds := Mux(core.reset, resetLED, trafficLED)
    }
  }
}

// Allow frequency of the design to be controlled by the Makefile
class WithFrequency(MHz: Double) extends Config((site, here, up) => {
  case IOFPGAFrequencyKey => MHz
})

class With50MHz extends WithFrequency(50)
class With100MHz extends WithFrequency(100)
class With125MHz extends WithFrequency(125)
class With150MHz extends WithFrequency(150)
class With200MHz extends WithFrequency(200)

class IOFPGAConfig extends Config(
  new FreedomU500Config().alter((site, here, up) => {
    case DesignKey => { (p:Parameters) => new IOFPGADesign()(p) }
  }))
