// See LICENSE for license details.
package sifive.freedom.unleashed.u500polarfireavalanchekit

import Chisel._
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.devices.microsemi.polarfireddr3._
import sifive.fpgashells.shell.microsemi.polarfireavalanchekitshell._

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


class U500PolarFireAvalancheKitFPGAChip(implicit override val p: Parameters)
    extends PolarFireAvalancheKitShell
    with HasDDR3 {

  //-----------------------------------------------------------------------
  // DUT
  //-----------------------------------------------------------------------

  // Connect the clock to the 50 Mhz output from the PLL
  withClockAndReset(dut_clock, dut_reset) {
    val dut = Module(LazyModule(new U500PolarFireAvalancheKitSystem).module)

    //---------------------------------------------------------------------
    // Connect peripherals
    //---------------------------------------------------------------------

    connectDebugJTAG(dut)
    connectUART     (dut)
    connectMIG      (dut)

    //---------------------------------------------------------------------
    // GPIO
    //---------------------------------------------------------------------

    val gpioParams = p(PeripheryGPIOKey)
    val gpio_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(0)))

    GPIOPinsFromPort(gpio_pins, dut.gpio(0))

    gpio_pins.pins.foreach { _.i.ival := Bool(false) }
    gpio_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => led(idx) := pin.o.oval
    }

    // tie to zero
    for( idx <- 7 to 4 ) { led(idx) := false.B }
  }

}
