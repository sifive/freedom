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
class IOFPGADesign()(implicit p: Parameters) extends LazyModule with BindingScope
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
        TLC =  AddressSet.misaligned(0x60000000L,   0x20000000L) ++                 // local memory behind L2 [1.5GB, 2GB)
               AddressSet.misaligned(0x80000000L,   0x2000000000L - 0x80000000L) ++ // U540 DDR               [  2GB, 128GB)
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
  val serr = LazyModule(new TLError(DevNullParams(Seq(AddressSet(0x2f00000000L, 0x7fffffffL)), 8, 128, region = RegionType.TRACKED), beatBytes = 8))
  val dtbrom = LazyModule(new TLROM(0x2ff0000000L, 0x10000, dtb.contents, executable = false, beatBytes = 8))
  val msimaster = LazyModule(new MSIMaster(Seq(MSITarget(address=0x2020000, spacing=4, number=10))))
  // We only support the first DDR or PCIe controller in this design
  val ddr = p(DDROverlayKey).headOption.map(_(DDROverlayParams(0x3000000000L, wrangler.node)))
  val pcieControllers = p(PCIeOverlayKey).size
  val lowBarSize = if (pcieControllers == 1) 0x20000000 else 0x10000000
  val pcieAddrs = Seq(
    (0x2c00000000L, Seq(AddressSet(0x2000000000L, 0x3ffffffffL), AddressSet(0x40000000, lowBarSize-1))),
    (0x2d00000000L, Seq(AddressSet(0x2400000000L, 0x3ffffffffL), AddressSet(0x40000000+lowBarSize, lowBarSize-1))),
    (0x2e00000000L, Seq(AddressSet(0x2400000000L, 0x3ffffffffL))))
  val (pcie, pcieInt) = p(PCIeOverlayKey).zipWithIndex.map { case (overlay, i) =>
    pcieAddrs(i) match { case (ecam, bars) => overlay(PCIeOverlayParams(wrangler.node, bars, ecam)) }
  }.unzip
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
  pcie.foreach { n => mbar.node := FlipRendering { implicit p => TLFIFOFixer() := n } }

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
  if (ddr.isEmpty) {
    val sram = LazyModule(new TLRAM(AddressSet(0x2f90000000L, 0xfff), beatBytes = 8))
    sram.node := TLFragmenter(8, 64) := sbar.node
  }
  ddr.foreach { _ := sbar.node }
  pcie.foreach { _ :*= TLWidthWidget(8) :*= sbar.node }

  // interrupts are fed into chiplink via MSI
  pcieInt.foreach { msimaster.intNode := _ }

  // Include optional NVDLA config
  val nvdla = p(NVDLAKey).map { config =>
    val nvdla = LazyModule(new NVDLA(config))
    val dlaGroup = ClockGroup()
    val dlaClock = ClockSinkNode(freqMHz = 400.0/3)
    dlaClock := wrangler.node := dlaGroup := corePLL

    FlipRendering { implicit p => mbar.node := TLFIFOFixer() } := TLWidthWidget(8) := nvdla.crossTLOut(nvdla.dbb_tl_node)
    nvdla.crossTLIn(nvdla.cfg_tl_node := nvdla { TLFragmenter(4, 64) := TLWidthWidget(8) }) := sbar.node
    msimaster.intNode := nvdla.crossIntOut(nvdla.int_node)

    InModuleBody {
      val (domain, _) = dlaClock.in(0)
      nvdla.module.clock := domain.clock
      nvdla.module.reset := domain.reset
    }
    nvdla
  }

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

class With100MHz extends WithFrequency(100)
class With125MHz extends WithFrequency(125)
class With150MHz extends WithFrequency(150)
class With200MHz extends WithFrequency(200)

class WithNVDLA(config: String) extends Config((site, here, up) => {
  case NVDLAKey => Some(NVDLAParams(config = config, raddress = 0x2f80000000L))
})

class WithNVDLALarge extends WithNVDLA("large")
class WithNVDLASmall extends WithNVDLA("small")

class IOFPGAConfig extends Config(
  new FreedomU500Config().alter((site, here, up) => {
    case DesignKey => { (p:Parameters) => new IOFPGADesign()(p) }
  }))
