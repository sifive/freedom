// See LICENSE for license details.
package sifive.freedom.everywhere.e300artydevkit

import Chisel._
import config._
import diplomacy._
import coreplex._
import rocketchip._
import uncore.devices.DebugBusIO
import sifive.blocks.devices.gpio.{GPIOConfig, PeripheryGPIO, PeripheryGPIOBundle, PeripheryGPIOModule, GPIOPin, GPIOPinToIOF, GPIOPinIOFCtrl, GPIOInputPinCtrl, JTAGPinsIO, JTAGGPIOPort}
import sifive.blocks.devices.mockaon.{MockAONConfig, PeripheryMockAON, PeripheryMockAONBundle, PeripheryMockAONModule, MockAONWrapperPadsIO}
import sifive.blocks.devices.pwm.{PWMConfig, PeripheryPWM, PeripheryPWMBundle, PeripheryPWMModule, PWMGPIOPort}
import sifive.blocks.devices.spi.{SPIConfig, PeripherySPI, PeripherySPIBundle, PeripherySPIModule, SPIFlashConfig, PeripherySPIFlash, PeripherySPIFlashBundle, PeripherySPIFlashModule, SPIPinsIO, SPIGPIOPort}
import sifive.blocks.devices.uart.{UARTConfig, PeripheryUART, PeripheryUARTBundle, PeripheryUARTModule, UARTGPIOPort}
import sifive.blocks.util.ResetCatchAndSync
import util._

// Coreplex and Periphery

trait E300ArtyDevKitPeripheryConfigs {
  val mockAONConfig = MockAONConfig(address = 0x10000000)
  val gpioConfig = GPIOConfig(address = 0x10012000, width = 32)
  val pwmConfigs = List(
    PWMConfig(address = 0x10015000, cmpWidth = 8),
    PWMConfig(address = 0x10025000, cmpWidth = 16),
    PWMConfig(address = 0x10035000, cmpWidth = 16))
  val spiConfigs = List(
    SPIConfig(csWidth = 4, rAddress = 0x10024000, sampleDelay = 3),
    SPIConfig(csWidth = 1, rAddress = 0x10034000, sampleDelay = 3))
  val spiFlashConfig = SPIFlashConfig(
    fAddress = 0x20000000, rAddress = 0x10014000, sampleDelay = 3)
  val uartConfigs = List(
    UARTConfig(address = 0x10013000),
    UARTConfig(address = 0x10023000))
}

// This custom E300ArtyDevKit coreplex has no port into the L2 and no memory subsystem

class E300ArtyDevKitCoreplex(implicit p: Parameters) extends BareCoreplex
    with CoreplexNetwork
    with CoreplexRISCVPlatform
    with RocketTiles {
  override lazy val module = new E300ArtyDevKitCoreplexModule(this, () => new E300ArtyDevKitCoreplexBundle(this))
}

class E300ArtyDevKitCoreplexBundle[+L <: E300ArtyDevKitCoreplex](_outer: L) extends BareCoreplexBundle(_outer)
    with CoreplexNetworkBundle
    with CoreplexRISCVPlatformBundle
    with RocketTilesBundle

class E300ArtyDevKitCoreplexModule[+L <: E300ArtyDevKitCoreplex, +B <: E300ArtyDevKitCoreplexBundle[L]](_outer: L, _io: () => B)
  extends BareCoreplexModule(_outer, _io)
    with CoreplexNetworkModule
    with CoreplexRISCVPlatformModule
    with RocketTilesModule

class E300ArtyDevKitSystem(implicit p: Parameters) extends BaseTop
    with E300ArtyDevKitPeripheryConfigs
    with PeripheryBootROM
    with PeripheryDebug
    with PeripheryMockAON
    with PeripheryUART
    with PeripherySPIFlash
    with PeripherySPI
    with PeripheryGPIO
    with PeripheryPWM
    with HardwiredResetVector {
  override lazy val module = new E300ArtyDevKitSystemModule(this, () => new E300ArtyDevKitSystemBundle(this))

  val coreplex = LazyModule(new E300ArtyDevKitCoreplex)
  socBus.node := coreplex.mmio
  coreplex.mmioInt := intBus.intnode
}

class E300ArtyDevKitSystemBundle[+L <: E300ArtyDevKitSystem](_outer: L) extends BaseTopBundle(_outer)
    with E300ArtyDevKitPeripheryConfigs
    with PeripheryBootROMBundle
    with PeripheryDebugBundle
    with PeripheryUARTBundle
    with PeripherySPIBundle
    with PeripheryGPIOBundle
    with PeripherySPIFlashBundle
    with PeripheryMockAONBundle
    with PeripheryPWMBundle
    with HardwiredResetVectorBundle

class E300ArtyDevKitSystemModule[+L <: E300ArtyDevKitSystem, +B <: E300ArtyDevKitSystemBundle[L]](_outer: L, _io: () => B)
  extends BaseTopModule(_outer, _io)
    with E300ArtyDevKitPeripheryConfigs
    with PeripheryBootROMModule
    with PeripheryDebugModule
    with PeripheryUARTModule
    with PeripherySPIModule
    with PeripheryGPIOModule
    with PeripherySPIFlashModule
    with PeripheryMockAONModule
    with PeripheryPWMModule
    with HardwiredResetVectorModule

// Top

class E300ArtyDevKitTopIO(implicit val p: Parameters) extends Bundle with E300ArtyDevKitPeripheryConfigs {
  val pads = new Bundle {
    val jtag = new JTAGPinsIO
    val gpio = Vec(gpioConfig.width, new GPIOPin)
    val qspi = new SPIPinsIO(spiFlashConfig)
    val aon = new MockAONWrapperPadsIO()
  }
}

class E300ArtyDevKitTop(implicit val p: Parameters) extends Module with E300ArtyDevKitPeripheryConfigs {
  val sys = Module(LazyModule(new E300ArtyDevKitSystem).module)
  val io = new E300ArtyDevKitTopIO

  // This needs to be de-asserted synchronously to the coreClk.
  val async_corerst = sys.io.aon.rsts.corerst
  sys.reset := ResetCatchAndSync(clock, async_corerst, 20)

  // ------------------------------------------------------------
  // Check for unsupported RCT Connections
  // ------------------------------------------------------------

  require (p(NExtTopInterrupts) == 0, "No Top-level interrupts supported");

  // ------------------------------------------------------------
  // Build GPIO Pin Mux
  // ------------------------------------------------------------

  // Pin Mux for UART, SPI, PWM
  // First convert the System outputs into "IOF" using the respective *GPIOPort
  // converters.
  val sys_uarts = sys.io.uarts
  val sys_pwms  = sys.io.pwms
  val sys_spis   = sys.io.spis

  val uart_pins = uartConfigs.map { c => Module (new UARTGPIOPort) }
  val pwm_pins  = pwmConfigs.map  { c => Module (new PWMGPIOPort(c.bc)) }
  val spi_pins  = spiConfigs.map  { c => Module (new SPIGPIOPort(c)) }

  (uart_pins zip sys_uarts) map {case (p, r) => p.io.uart <> r}
  (pwm_pins zip  sys_pwms)  map {case (p, r) => p.io.pwm  <> r}
  (spi_pins zip  sys_spis)  map {case (p, r) => p.io.spi <> r}

  // ------------------------------------------------------------
  // Default Pin connections before attaching pinmux

  for (iof_0 <- sys.io.gpio.iof_0) {
    iof_0.o := GPIOPinIOFCtrl()
  }

  for (iof_1 <- sys.io.gpio.iof_1) {
    iof_1.o := GPIOPinIOFCtrl()
  }

  // ------------------------------------------------------------
  // TODO: Make this mapping more programmatic.

  val iof_0 = sys.io.gpio.iof_0
  val iof_1 = sys.io.gpio.iof_1

  // SPI1 (0 is the dedicated)
  GPIOPinToIOF(spi_pins(0).io.pins.cs(0), iof_0(2))
  GPIOPinToIOF(spi_pins(0).io.pins.dq(0), iof_0(3))
  GPIOPinToIOF(spi_pins(0).io.pins.dq(1), iof_0(4))
  GPIOPinToIOF(spi_pins(0).io.pins.sck,   iof_0(5))
  GPIOPinToIOF(spi_pins(0).io.pins.dq(2), iof_0(6))
  GPIOPinToIOF(spi_pins(0).io.pins.dq(3), iof_0(7))
  GPIOPinToIOF(spi_pins(0).io.pins.cs(1), iof_0(8))
  GPIOPinToIOF(spi_pins(0).io.pins.cs(2), iof_0(9))
  GPIOPinToIOF(spi_pins(0).io.pins.cs(3), iof_0(10))

  // SPI2
  GPIOPinToIOF(spi_pins(1).io.pins.cs(0), iof_0(26))
  GPIOPinToIOF(spi_pins(1).io.pins.dq(0), iof_0(27))
  GPIOPinToIOF(spi_pins(1).io.pins.dq(1), iof_0(28))
  GPIOPinToIOF(spi_pins(1).io.pins.sck,   iof_0(29))
  GPIOPinToIOF(spi_pins(1).io.pins.dq(2), iof_0(30))
  GPIOPinToIOF(spi_pins(1).io.pins.dq(3), iof_0(31))

  // UART0
  GPIOPinToIOF(uart_pins(0).io.pins.rxd, iof_0(16))
  GPIOPinToIOF(uart_pins(0).io.pins.txd, iof_0(17))

  // UART1
  GPIOPinToIOF(uart_pins(1).io.pins.rxd, iof_0(24))
  GPIOPinToIOF(uart_pins(1).io.pins.txd, iof_0(25))

  //PWM
  GPIOPinToIOF(pwm_pins(0).io.pins.pwm(0), iof_1(0) )
  GPIOPinToIOF(pwm_pins(0).io.pins.pwm(1), iof_1(1) )
  GPIOPinToIOF(pwm_pins(0).io.pins.pwm(2), iof_1(2) )
  GPIOPinToIOF(pwm_pins(0).io.pins.pwm(3), iof_1(3) )

  GPIOPinToIOF(pwm_pins(1).io.pins.pwm(1), iof_1(19))
  GPIOPinToIOF(pwm_pins(1).io.pins.pwm(0), iof_1(20))
  GPIOPinToIOF(pwm_pins(1).io.pins.pwm(2), iof_1(21))
  GPIOPinToIOF(pwm_pins(1).io.pins.pwm(3), iof_1(22))

  GPIOPinToIOF(pwm_pins(2).io.pins.pwm(0), iof_1(10))
  GPIOPinToIOF(pwm_pins(2).io.pins.pwm(1), iof_1(11))
  GPIOPinToIOF(pwm_pins(2).io.pins.pwm(2), iof_1(12))
  GPIOPinToIOF(pwm_pins(2).io.pins.pwm(3), iof_1(13))

  // ------------------------------------------------------------
  // Drive actual Pads
  // ------------------------------------------------------------

  // Result of Pin Mux
  io.pads.gpio <> sys.io.gpio.pins

  val dedicated_spi_pins = Module (new SPIGPIOPort(spiFlashConfig, syncStages=3, driveStrength=Bool(true)))
  dedicated_spi_pins.clock := sys.clock
  dedicated_spi_pins.reset := sys.reset
  io.pads.qspi <> dedicated_spi_pins.io.pins
  dedicated_spi_pins.io.spi <> sys.io.qspi

  // JTAG Debug Interface

  val jtag_pins = Module (new JTAGGPIOPort(true))
  io.pads.jtag <> jtag_pins.io.pins
  sys.io.jtag.get <> jtag_pins.io.jtag
  // Override TRST to reset this logic IFF the core is in reset.
  // This will require 3 ticks of TCK before the debug logic
  // comes out of reset, but JTAG needs 5 ticks anyway.
  // This means that the "real" TRST is never actually used in this design.
  sys.io.jtag.get.TRST := ResetCatchAndSync(sys.io.jtag.get.TCK, async_corerst)

  // AON Pads
  io.pads.aon <> sys.io.aon.pads
}
