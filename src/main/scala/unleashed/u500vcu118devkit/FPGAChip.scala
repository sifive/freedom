// See LICENSE for license details.
package sifive.freedom.unleashed.u500vcu118devkit

import Chisel._
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.shell.xilinx.vcu118shell._
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
// U500VCU118DevKitFPGAChip
//-------------------------------------------------------------------------

class U500VCU118DevKitFPGAChip(implicit override val p: Parameters)
    extends VCU118Shell
    with HasDDR3 {

  //-----------------------------------------------------------------------
  // DUT
  //-----------------------------------------------------------------------

  // Connect the clock to the 50 Mhz output from the PLL
  dut_clock := clk50
  withClockAndReset(dut_clock, dut_reset) {
    val dut = Module(LazyModule(new U500VCU118DevKitSystem).module)

    //---------------------------------------------------------------------
    // Connect peripherals
    //---------------------------------------------------------------------

    connectDebugJTAG(dut)
    connectSPI      (dut)
    connectUART     (dut)
    connectMIG      (dut)

    //---------------------------------------------------------------------
    // GPIO
    //---------------------------------------------------------------------

    val gpioParams = p(PeripheryGPIOKey)
    val gpio_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(0)))

    GPIOPinsFromPort(gpio_pins, dut.gpio(0))

    gpio_pins.pins.foreach { _.i.ival := Bool(false) }
    //gpio_pins.pins.zipWithIndex.foreach {
    //  case(pin, idx) => led(idx) := pin.o.oval
    //}

    // tie to zero
    //for( idx <- 7 to 4 ) { led(idx) := false.B }
  }

}
