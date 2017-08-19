// See LICENSE for license details.
package sifive.freedom.unleashed.u500vc707devkit

import Chisel._
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.shell.xilinx.vc707shell.{VC707Shell}
import sifive.fpgashells.ip.xilinx.{IOBUF}

//-------------------------------------------------------------------------
// PinGen
//-------------------------------------------------------------------------

object PinGen {
  def apply(): BasePin = {
    new BasePin()
  }
}

//-------------------------------------------------------------------------
// U500VC707DevKitFPGAChip
//-------------------------------------------------------------------------

class U500VC707DevKitFPGAChip(implicit override val p: Parameters) extends VC707Shell {

  //-----------------------------------------------------------------------
  // DUT
  //-----------------------------------------------------------------------

  // Connect the clock to the 50 Mhz output from the PLL
  dut_clock := clk50
  withClockAndReset(dut_clock, dut_reset) {
    val dut = Module(LazyModule(new U500VC707DevKitSystem).module)

    //---------------------------------------------------------------------
    // Connect peripherals
    //---------------------------------------------------------------------

    connectDebugJTAG(dut)
    connectSPI      (dut)
    connectUART     (dut)
    connectPCIe     (dut)
    connectMIG      (dut)

    //---------------------------------------------------------------------
    // GPIO
    //---------------------------------------------------------------------

    val gpioParams = p(PeripheryGPIOKey)
    val gpio_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(0)))

    gpio_pins.fromPort(dut.gpio(0))

    gpio_pins.pins.foreach { _.i.ival := Bool(false) }
    gpio_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => led(idx) := pin.o.oval
    }

    // tie to zero
    for( idx <- 7 to 4 ) { led(idx) := false.B }
  }

}
