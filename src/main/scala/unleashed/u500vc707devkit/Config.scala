// See LICENSE for license details.
package sifive.freedom.unleashed.u500vc707devkit

import freechips.rocketchip.config._
import freechips.rocketchip.coreplex._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._

// Default FreedomUVC707Config
class FreedomUVC707Config extends Config(
  new WithJtagDTM            ++
  new WithNMemoryChannels(1) ++
  new WithNBigCores(1)       ++
  new BaseConfig
)

// Freedom U500 VC707 Dev Kit Peripherals
class U500VC707DevKitPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(
    UARTParams(address = BigInt(0x54000000L)))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x54001000L)))
  case PeripheryGPIOKey => List(
    GPIOParams(address = BigInt(0x54002000L), width = 4))
  case PeripheryMaskROMKey => List(
    MaskROMParams(address = 0x10000, name = "BootROM"))
})

// Freedom U500 VC707 Dev Kit
class U500VC707DevKitConfig extends Config(
  new WithoutFPU                 ++
  new WithNExtTopInterrupts(0)   ++
  new U500VC707DevKitPeripherals ++
  new FreedomUVC707Config().alter((site,here,up) => {
    case ErrorParams => ErrorParams(Seq(AddressSet(0x3000, 0xfff)))
    case PeripheryBusParams => up(PeripheryBusParams, site).copy(frequency = 50000000) // 50 MHz hperiphery
    case DTSTimebase => BigInt(1000000)
    case ExtMem => up(ExtMem).copy(size = 0x40000000L)
    case JtagDTMKey => new JtagDTMConfig (
      idcodeVersion = 2,      // 1 was legacy (FE310-G000, Acai).
      idcodePartNum = 0x000,  // Decided to simplify.
      idcodeManufId = 0x489,  // As Assigned by JEDEC to SiFive. Only used in wrappers / test harnesses.
      debugIdleCycles = 5)    // Reasonable guess for synchronization
  })
)
