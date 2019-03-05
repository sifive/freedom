// See LICENSE for license details.
package sifive.freedom.unleashed.u500polarfireavalanchekit

import freechips.rocketchip.config._
//import freechips.rocketchip.coreplex._
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

// Default FreedomU PolarFire Avalanche Kit Config
class FreedomUPolarFireAvalancheKitConfig  extends Config(
  new WithJtagDTM            ++
  new WithNMemoryChannels(1) ++
  new WithNBigCores(1)       ++
  new BaseConfig
)

// Freedom U500 PolarFire Eval Kit Peripherals
class U500PolarFireAvalancheKitPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(
    UARTParams(address = BigInt(0x64000000L)))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x64001000L)))
  case PeripheryGPIOKey => List(
    GPIOParams(address = BigInt(0x64002000L), width = 4))
})

// Freedom U500 PolarFire Avalanche Kit
class U500PolarFireAvalancheKitConfig extends Config(
  new WithNExtTopInterrupts(0)   ++
  new U500PolarFireAvalancheKitPeripherals ++
  new FreedomUPolarFireAvalancheKitConfig ().alter((site,here,up) => {
    case PeripheryBusKey => up(PeripheryBusKey, site).copy(frequency = 50000000) // 50 MHz hperiphery
    case MemoryMicrosemiDDR3Key => PolarFireEvalKitDDR3Params(address = Seq(AddressSet(0x80000000L,0x40000000L-1))) //1GB
    case DTSTimebase => BigInt(1000000)
    case JtagDTMKey => new JtagDTMConfig (
      idcodeVersion = 2,      // 1 was legacy (FE310-G000, Acai).
      idcodePartNum = 0x000,  // Decided to simplify.
      idcodeManufId = 0x489,  // As Assigned by JEDEC to SiFive. Only used in wrappers / test harnesses.
      debugIdleCycles = 5)    // Reasonable guess for synchronization
  })
)
