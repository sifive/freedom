// See LICENSE for license details.

package sifive.freedom.unleashed.vera.iofpga

import Chisel._
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util.{ElaborationArtefacts,ResetCatchAndSync}

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pinctrl.{BasePin}
import sifive.blocks.devices.msi._
import sifive.blocks.devices.chiplink._

import sifive.fpgashells.shell.microsemi.verashell.{VeraShell,HasPCIe,HasDDR3,HasPFEvalKitChipLink}
import sifive.fpgashells.devices.microsemi.polarfireevalkitpciex4._
import sifive.freedom.unleashed.FreedomU500Config
import sifive.fpgashells.ip.microsemi.CLKINT
import sifive.fpgashells.ip.microsemi.polarfiredll._
import sifive.fpgashells.ip.microsemi.polarfireccc._

import sifive.fpgashells.devices.microsemi.polarfireddr4._
import sifive.fpgashells.clocks._

//-------------------------------------------------------------------------
// PinGen
//-------------------------------------------------------------------------

object PinGen {
  def apply(): BasePin = {
    new BasePin()
  }
}

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

class IOFPGA(
  localRoute:     Seq[AddressSet],
  ddrparams:      PolarFireEvalKitDDR4Params,
  chiplinkparams: ChipLinkParams,
  gpioparams:     GPIOParams)(implicit p: Parameters) extends LazyModule
{
  val link = LazyModule(new ChipLink(chiplinkparams))
  val linksink = link.ioNode.makeSink
  val sbar = LazyModule(new TLXbar)
  val xbar = LazyModule(new TLXbar)
  val mbar = LazyModule(new TLXbar)
  val serr = LazyModule(new TLError(DevNullParams(Seq(AddressSet(0x2800000000L, 0xffffffffL)), 8, 128), beatBytes = 8))
  val gpio = LazyModule(new TLGPIO(busWidthBytes = 8, params = gpioparams))
  val gpiosink = gpio.ioNode.makeSink
  val polarfirepcie = LazyModule(new PolarFireEvalKitPCIeX4)
  val msimaster = LazyModule(new MSIMaster(Seq(MSITarget(address=0x2020000, spacing=4, number=10))))
  val test_ram  = LazyModule(new TLRAM(AddressSet(0x2500000000L, 0x3fff), beatBytes = 8))
  val pf_ddr4 = LazyModule(new PolarFireEvalKitDDR4(ddrparams))
  
  private def filter(m: TLManagerParameters) = // keep only managers that are locally routed
    if (m.address.exists(a => localRoute.exists(_.overlaps(a)))) Some(m) else None

  // local master Xbar
  mbar.node := msimaster.masterNode
  mbar.node := TLFIFOFixer() := polarfirepcie.crossTLOut(polarfirepcie.master)

  // split local master traffic either to local routing or off-chip
  link.node := TLBuffer() := mbar.node
  xbar.node := TLFilter(filter) := TLBuffer() := mbar.node
  xbar.node := TLBuffer() := link.node

  // receive traffic either from local routing or from off-chip
  sbar.node := TLBuffer() := TLAtomicAutomata() := TLFIFOFixer() := TLHintHandler() := TLBuffer() := TLWidthWidget(4) := xbar.node

  // local slave Xbar
  serr.node := sbar.node


  gpio.controlXing(NoCrossing) := TLFragmenter(8,64,true) := sbar.node
  polarfirepcie.crossTLIn(polarfirepcie.slave) := TLWidthWidget(8) := sbar.node
  polarfirepcie.crossTLIn(polarfirepcie.control) := TLWidthWidget(8) := sbar.node
  test_ram.node := TLFragmenter(8, 64) := sbar.node
  pf_ddr4.node := sbar.node

  // interrupts are fed into chiplink via MSI
  msimaster.intNode := polarfirepcie.crossIntOut(polarfirepcie.intnode)
  msimaster.intNode := gpio.intXing(NoCrossing)

  lazy val module = new LazyModuleImp(this) {
    val io = IO (new Bundle {
      val tx_clock = Clock(INPUT)
      val chiplink = new WideDataLayerPort(chiplinkparams)
      val gpio = new GPIOPortIO(gpioparams)
      val polarfirepcie = new PolarFireEvalKitPCIeX4IO
      val link_up  = Bool(OUTPUT)
      val dll_up = Bool(OUTPUT)
      val pf_ddr4 = new PolarFireEvalKitDDR4IO(AddressRange.fromSets(ddrparams.address).head.size)
    })

    io.polarfirepcie <> polarfirepcie.module.io.port
    io.pf_ddr4 <> pf_ddr4.module.io.port

    io.chiplink <> linksink.bundle

    // Take the b2c clock from an input pin
    val chiplink_rx_clkint = Module(new CLKINT)
    chiplink_rx_clkint.io.A := io.chiplink.b2c.clk

    // Skew the RX clock to sample in the data eye
    val chiplink_rx_pll = Module(new PolarFireCCC(PLLParameters(
        name = "chiplink_rx_pll",
        PLLInClockParameters(125), 
        Seq(PLLOutClockParameters(freqMHz=125),
        PLLOutClockParameters(freqMHz=125, phaseDeg = 240)))))

    val lock = chiplink_rx_pll.io.PLL_LOCK_0
    chiplink_rx_pll.io.REF_CLK_0 := chiplink_rx_clkint.io.Y
    linksink.bundle.b2c.clk := chiplink_rx_pll.io.OUT1_FABCLK_0.get

    // Use a phase-adjusted c2b_clk to meet timing constraints
    io.chiplink.c2b.clk := io.tx_clock

    // Hold ChipLink in reset for a bit after power-on
    val timer = RegInit(UInt(255, width=8))
    timer := timer - (timer.orR && lock)

    link.module.io.c2b_clk := clock
    link.module.io.c2b_rst := ResetCatchAndSync(clock, reset || timer.orR)

    io.gpio <> gpiosink.bundle

    // Report link-up once RX is out of reset
    io.link_up := !io.chiplink.b2c.rst
    io.dll_up  := lock
  }
}

class IOFPGAChip(implicit override val p: Parameters) extends VeraShell
  with HasPFEvalKitChipLink {

  val chipLinkParams = ChipLinkParams(
        TLUH = AddressSet.misaligned(0,             0x40000000L),                   // Aloe MMIO              [  0GB, 1GB)
        TLC =  AddressSet.misaligned(0x60000000L,   0x20000000L) ++                 // local memory behind L2 [1.5GB, 2GB)
               AddressSet.misaligned(0x80000000L,   0x2000000000L - 0x80000000L) ++ // Aloe DDR               [  2GB, 128GB)
               AddressSet.misaligned(0x3000000000L, 0x1000000000L),                 // local memory behind L2 [192GB, 256GB)
        syncTX = true
  )
  val localRoute = AddressSet.misaligned(0x40000000L, 0x20000000L) ++               // local MMIO             [  1GB, 1.5GB)
                   AddressSet.misaligned(0x2000000000L, 0x1000000000L)              // local MMIO             [128GB, 192GB)
//  val gpioParams = GPIOParams(address = BigInt(0x2400000000L), width = 4)
  val gpioParams = GPIOParams(address = BigInt(0x2400000000L), width = 8)
  val ddrParams = PolarFireEvalKitDDR4Params(address = Seq(AddressSet(0x2600000000L,0x40000000L-1)))

  //-----------------------------------------------------------------------
  // DUT
  //-----------------------------------------------------------------------

  // System runs at 125 MHz
  dut_clock := hart_clk_125
  dut_ext_reset_n := ereset_n

  val pcie = IO(new PolarFireEvalKitPCIeX4Pads)
  val ddr = IO(new PolarFireEvalKitDDR4Pads(ddrParams))

  withClockAndReset(dut_clock, dut_reset) {

    // PCIe switch reset:
    val timer = RegInit(UInt(268435456, width=29))
//    val timer = RegInit(UInt(16383, width=15))
    timer := timer - timer.orR
  
    val pf_rstb_i = !ResetCatchAndSync(pcie_fab_ref_clk, !sys_reset_n || timer.orR)
    led3            := pf_rstb_i
    pf_rstb         := pf_rstb_i
    
    // PCIe slots reset
    perst_x1_slot   := pf_rstb_i
    perst_x16_slot  := pf_rstb_i
    perst_m2_slot   := pf_rstb_i
    perst_sata_slot := pf_rstb_i

//    val iofpga = Module(LazyModule(new IOFPGA(localRoute,ddrParams,chipLinkParams,gpioParams)).module)
//    val iofpga = Module(LazyModule(new IOFPGA(localRoute,chipLinkParams,gpioParams)).module)
    val iofpga = Module(LazyModule(new IOFPGA(localRoute,ddrParams,chipLinkParams,gpioParams)).module)

    //---------------------------------------------------------------------
    // PCIe
    //---------------------------------------------------------------------
    iofpga.io.polarfirepcie.APB_S_PCLK     := dut_clock
   
    iofpga.io.polarfirepcie.APB_S_PRESET_N := sys_reset_n   //!dut_reset //UInt("b1")
    
    iofpga.io.polarfirepcie.AXI_CLK        := dut_clock
    iofpga.io.polarfirepcie.AXI_CLK_STABLE := hart_clk_lock
    
    iofpga.io.polarfirepcie.PCIE_1_TL_CLK_125MHz   := pcie_tl_clk
    
    iofpga.io.polarfirepcie.PCIE_1_TX_PLL_REF_CLK  := pf_tx_pll_refclk_to_lane

    iofpga.io.polarfirepcie.PCIE_1_TX_BIT_CLK := pf_tx_pll_bitclk
    
    iofpga.io.polarfirepcie.PCIESS_LANE0_CDR_REF_CLK_0 := pcie_refclk
    iofpga.io.polarfirepcie.PCIESS_LANE1_CDR_REF_CLK_0 := pcie_refclk
    iofpga.io.polarfirepcie.PCIESS_LANE2_CDR_REF_CLK_0 := pcie_refclk
    iofpga.io.polarfirepcie.PCIESS_LANE3_CDR_REF_CLK_0 := pcie_refclk

    iofpga.io.polarfirepcie.PCIE_1_TX_PLL_LOCK := pf_tx_pll_lock


    pcie <> iofpga.io.polarfirepcie

    // <CJ> debug
    debug_io0 := iofpga.io.polarfirepcie.debug_pclk
    debug_io1 := iofpga.io.polarfirepcie.debug_preset
    debug_io2 := iofpga.io.polarfirepcie.debug_penable
    debug_io3 := iofpga.io.polarfirepcie.debug_psel
    debug_io4 := iofpga.io.polarfirepcie.debug_paddr2
    debug_io5 := iofpga.io.polarfirepcie.debug_paddr3
    
     //---------------------------------------------------------------------
    // DDR
    //---------------------------------------------------------------------
    iofpga.io.pf_ddr4.PLL_REF_CLK := ref_clk_int.io.Y
    iofpga.io.pf_ddr4.SYS_RESET_N := sys_reset_n
    
//    := iofpga.io.pf_ddr4.SYS_CLK
    ddr_pll_lock  := iofpga.io.pf_ddr4.PLL_LOCK
    
    ddr_ready  := iofpga.io.pf_ddr4.CTRLR_READY
    led5 := ddr_ready
    
    ddr <> iofpga.io.pf_ddr4

   
    //---------------------------------------------------------------------
    // ChipLink
    //---------------------------------------------------------------------
    iofpga.io.tx_clock := hart_clk_125_tx
    chiplink <> iofpga.io.chiplink
    
    constrainChipLink(iofpga=true)

    val counter = RegInit(UInt(255))
    counter := counter - counter.orR

    led2 := iofpga.io.link_up
    led3 := iofpga.io.dll_up
    led4 := counter === UInt(0)
//    led5 := Bool(true)
    
    //---------------------------------------------------------------------
    // GPIO
    //---------------------------------------------------------------------
    val gpio_pins = Wire(new GPIOPins(() => PinGen(), gpioParams))

    GPIOPinsFromPort(gpio_pins, iofpga.io.gpio)

    gpio_pins.pins(0).i.ival := Bool(false)
    gpio_pins.pins(1).i.ival := Bool(true)
    gpio_pins.pins(2).i.ival := Bool(false)
    gpio_pins.pins(3).i.ival := Bool(true)
    gpio_pins.pins(4).i.ival := Bool(false)
    gpio_pins.pins(5).i.ival := Bool(false)
    gpio_pins.pins(6).i.ival := Bool(true)
    gpio_pins.pins(7).i.ival := Bool(true)
  }

}

class IOFPGAConfig extends Config(new FreedomU500Config)
