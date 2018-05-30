// See LICENSE for license details.

package sifive.freedom.unleashed.vc707.iofpga

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

import sifive.fpgashells.shell.xilinx.vc707shell.{VC707Shell,HasPCIe,HasDDR3,HasVC707ChipLink}
import sifive.fpgashells.ip.xilinx.{IOBUF,Series7MMCM}
import sifive.fpgashells.devices.xilinx.xilinxvc707mig._
import sifive.fpgashells.devices.xilinx.xilinxvc707pciex1._
import sifive.fpgashells.clocks._

import sifive.freedom.unleashed.u500vc707devkit.FreedomUVC707Config


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
  ddrparams:      XilinxVC707MIGParams,
  chiplinkparams: ChipLinkParams,
  gpioparams:     GPIOParams)(implicit p: Parameters) extends LazyModule
{
  val link = LazyModule(new ChipLink(chiplinkparams))
  val sbar = LazyModule(new TLXbar)
  val xbar = LazyModule(new TLXbar)
  val mbar = LazyModule(new TLXbar)
  val serr = LazyModule(new TLError(ErrorParams(Seq(AddressSet(0x2800000000L, 0xffffffffL)), 8, 128, true), beatBytes = 8))
  val gpio = LazyModule(new TLGPIO(w = 8, c = gpioparams))
  val xilinxvc707mig = LazyModule(new XilinxVC707MIG(ddrparams))
  val xilinxvc707pcie = LazyModule(new XilinxVC707PCIeX1)
  val hack = LazyModule(new ShadowRAMHack)
  val msimaster = LazyModule(new MSIMaster(Seq(MSITarget(address=0x2020000, spacing=4, number=10))))

  private def filter(m: TLManagerParameters) = // keep only managers that are locally routed
    if (m.address.exists(a => localRoute.exists(_.overlaps(a)))) Some(m) else None

  // local master Xbar
  mbar.node := msimaster.masterNode
  mbar.node := TLFIFOFixer() := xilinxvc707pcie.crossTLOut := xilinxvc707pcie.master

  // split local master traffic either to local routing or off-chip
  link.node := TLBuffer() := mbar.node
  xbar.node := TLFilter(filter) := TLBuffer() := mbar.node
  xbar.node := TLBuffer() := link.node

  // receive traffic either from local routing or from off-chip
  sbar.node := TLBuffer() := TLAtomicAutomata() := TLFIFOFixer() := TLHintHandler() := TLBuffer() := TLWidthWidget(4) := xbar.node

  // local slave Xbar
  serr.node := sbar.node
  gpio.node := TLFragmenter(8,64,true) := sbar.node
  xilinxvc707mig.node := hack.node := sbar.node
  xilinxvc707pcie.slave := xilinxvc707pcie.crossTLIn := TLWidthWidget(8) := sbar.node
  xilinxvc707pcie.control := xilinxvc707pcie.crossTLIn := TLWidthWidget(8) := sbar.node

  // interrupts are fed into chiplink via MSI
  msimaster.intNode := xilinxvc707pcie.crossIntOut := xilinxvc707pcie.intnode
  msimaster.intNode := gpio.intnode

  lazy val module = new LazyModuleImp(this) {
    val io = IO (new Bundle {
      val chiplink = new WideDataLayerPort(chiplinkparams)
      val gpio = new GPIOPortIO(gpioparams)
      val xilinxvc707mig = new XilinxVC707MIGIO(AddressRange.fromSets(ddrparams.address).head.size)
      val xilinxvc707pcie = new XilinxVC707PCIeX1IO
      val rxlocked = Bool(INPUT)
    })

    io.xilinxvc707pcie <> xilinxvc707pcie.module.io.port
    xilinxvc707pcie.module.clock := xilinxvc707pcie.module.io.port.axi_aclk_out
    xilinxvc707pcie.module.reset := ~io.xilinxvc707pcie.axi_aresetn

    io.xilinxvc707mig <> xilinxvc707mig.module.io.port

    // Hold ChipLink in reset for a bit after power-on
    val timer = RegInit(UInt(255, width=8))
    timer := timer - timer.orR

    io.chiplink <> link.module.io.port
    link.module.io.c2b_clk := clock
    link.module.io.c2b_rst := ResetCatchAndSync(clock, reset || timer.orR || !io.rxlocked)

    io.gpio <> gpio.module.io.port
  }
}

class IOFPGAChip(implicit override val p: Parameters) extends VC707Shell
  with HasVC707ChipLink {

  val ddrParams = XilinxVC707MIGParams(address = Seq(AddressSet(0x3000000000L, 0xFFFFFFFFL)))  // 192GB - 196GB (behind L2);
  val chipLinkParams = ChipLinkParams(
        TLUH = AddressSet.misaligned(0,             0x40000000L),                   // Aloe MMIO              [  0GB, 1GB)
        TLC =  AddressSet.misaligned(0x60000000L,   0x20000000L) ++                 // local memory behind L2 [1.5GB, 2GB)
               AddressSet.misaligned(0x80000000L,   0x2000000000L - 0x80000000L) ++ // Aloe DDR               [  2GB, 128GB)
               AddressSet.misaligned(0x3000000000L, 0x1000000000L),                 // local memory behind L2 [192GB, 256GB)
        syncTX = true
  )
  val localRoute = AddressSet.misaligned(0x40000000L, 0x20000000L) ++               // local MMIO             [  1GB, 1.5GB)
                   AddressSet.misaligned(0x2000000000L, 0x1000000000L)              // local MMIO             [128GB, 192GB)
  val gpioParams = GPIOParams(address = BigInt(0x2400000000L), width = 4);

  // ChipLink skew RX clock
  val vc707_sys_clock_mmcm3 = Module(new Series7MMCM(PLLParameters(
      "vc707_sys_clock_mmcm3",
      InClockParameters(freqMHz=100), 
      Seq(OutClockParameters(freqMHz=100, phaseDeg=180)))))

  //-----------------------------------------------------------------------
  // DUT
  //-----------------------------------------------------------------------

  // System runs at 100 MHz
  dut_clock := clk100
  dut_ndreset := !ereset_n // debug reset is external

  val ddr = IO(new XilinxVC707MIGPads(ddrParams))
  val pcie = IO(new XilinxVC707PCIeX1Pads)

  withClockAndReset(dut_clock, dut_reset) {

    val iofpga = Module(LazyModule(new IOFPGA(localRoute,ddrParams,chipLinkParams,gpioParams)).module)

    //---------------------------------------------------------------------
    // DDR
    //---------------------------------------------------------------------
    iofpga.io.xilinxvc707mig.sys_clk_i := sys_clock.asUInt
    mig_clock                          := iofpga.io.xilinxvc707mig.ui_clk
    mig_sys_reset                      := iofpga.io.xilinxvc707mig.ui_clk_sync_rst
    mig_mmcm_locked                    := iofpga.io.xilinxvc707mig.mmcm_locked
    iofpga.io.xilinxvc707mig.aresetn   := mig_resetn
    iofpga.io.xilinxvc707mig.sys_rst   := sys_reset

    ddr <> iofpga.io.xilinxvc707mig

    //---------------------------------------------------------------------
    // PCIe
    //---------------------------------------------------------------------
    iofpga.io.xilinxvc707pcie.axi_aresetn     := pcie_dat_resetn
    pcie_dat_clock                            := iofpga.io.xilinxvc707pcie.axi_aclk_out
    pcie_cfg_clock                            := iofpga.io.xilinxvc707pcie.axi_ctl_aclk_out
    mmcm_lock_pcie                            := iofpga.io.xilinxvc707pcie.mmcm_lock
    iofpga.io.xilinxvc707pcie.axi_ctl_aresetn := pcie_dat_resetn

    pcie <> iofpga.io.xilinxvc707pcie

    //---------------------------------------------------------------------
    // ChipLink
    //---------------------------------------------------------------------

    chiplink <> iofpga.io.chiplink
    constrainChipLink(iofpga=true)

    chiplink.c2b.clk := clk100_180

    vc707_sys_clock_mmcm3.io.reset   := reset
    vc707_sys_clock_mmcm3.io.clk_in1 := chiplink.b2c.clk.asUInt.toBool
    iofpga.io.chiplink.b2c.clk := vc707_sys_clock_mmcm3.getClocks(0)



    iofpga.io.rxlocked := vc707_sys_clock_mmcm3.io.locked

    //---------------------------------------------------------------------
    // GPIO
    //---------------------------------------------------------------------
    val gpio_pins = Wire(new GPIOPins(() => PinGen(), gpioParams))

    GPIOPinsFromPort(gpio_pins, iofpga.io.gpio)

    gpio_pins.pins.foreach { _.i.ival := Bool(false) }
    gpio_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => led(idx) := pin.o.oval
    }

    // diagnostics
    led(4) := vc707_sys_clock_mmcm3.io.locked
    led(5) := ereset_n
    led(6) := iofpga.io.chiplink.b2c.send
    led(7) := iofpga.io.chiplink.c2b.send
  }

}

class IOFPGAConfig extends Config(new FreedomUVC707Config)
