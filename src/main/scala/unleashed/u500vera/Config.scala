// See LICENSE for license details.
package sifive.freedom.unleashed.u500vera

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._

import sifive.fpgashells.devices.microsemi.polarfireddr3.{MemoryMicrosemiDDR3Key, PolarFireEvalKitDDR3Params}
import sifive.fpgashells.devices.microsemi.polarfireddr4.{MemoryMicrosemiDDR4Key, PolarFireEvalKitDDR4Params}

// Default FreedomU PolarFire Eval Kit Config
class FreedomVeraConfig extends Config(
  new WithJtagDTM            ++
  new WithNMemoryChannels(1) ++
  new WithNBigCores(1)       ++
  new BaseConfig
)

// Freedom U500 PolarFire Eval Kit Peripherals
class U500VeraPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(
    UARTParams(address = BigInt(0x54000000L)))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x54001000L)))
  case PeripheryGPIOKey => List(
    GPIOParams(address = BigInt(0x54002000L), width = 4))
  case PeripheryMaskROMKey => List(
    MaskROMParams(address = 0x10000, name = "BootROM"))
})

// Freedom U500 PolarFire Eval Kit
class U500VeraConfig extends Config(
  new WithNExtTopInterrupts(0)   ++
  new U500VeraPeripherals ++
  new FreedomVeraConfig().alter((site,here,up) => {
    case ErrorParams => ErrorParams(Seq(AddressSet(0x3000, 0xfff)), maxAtomic=site(XLen)/8, maxTransfer=128)
    case PeripheryBusKey => up(PeripheryBusKey, site).copy(frequency = 50000000) // 50 MHz hperiphery
    case MemoryMicrosemiDDR3Key => PolarFireEvalKitDDR3Params(address = Seq(AddressSet(0x80000000L,0x40000000L-1))) //1GB
//    case MemoryMicrosemiDDR4Key => PolarFireEvalKitDDR4Params(address = Seq(AddressSet(0x80000000L,0x40000000L-1))) //1GB
    case DTSTimebase => BigInt(1000000)
    case ExtMem => up(ExtMem).map(_.copy(size = 0x40000000L))
    case JtagDTMKey => new JtagDTMConfig (
      idcodeVersion = 2,      // 1 was legacy (FE310-G000, Acai).
      idcodePartNum = 0x000,  // Decided to simplify.
      idcodeManufId = 0x489,  // As Assigned by JEDEC to SiFive. Only used in wrappers / test harnesses.
      debugIdleCycles = 5)    // Reasonable guess for synchronization
  })
)

