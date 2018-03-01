// See LICENSE for license details.
package sifive.freedom.unleashed.u500polarfireevalkit

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.coreplex._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._

import sifive.fpgashells.devices.microsemi.polarfireddr3._
import sifive.fpgashells.devices.microsemi.polarfireddr4._
import sifive.fpgashells.devices.microsemi.polarfireevalkitpciex4._

//-------------------------------------------------------------------------
// U500PolarFireEvalKitSystem
//-------------------------------------------------------------------------

class U500PolarFireEvalKitSystem(implicit p: Parameters) extends RocketCoreplex
    with HasPeripheryMaskROMSlave
    with HasPeripheryDebug
    with HasSystemErrorSlave
    with HasPeripheryUART
    with HasPeripherySPI
    with HasPeripheryGPIO
//    with HasMemoryPolarFireEvalKitDDR4
    with HasMemoryPolarFireEvalKitDDR3
    with HasSystemPolarFireEvalKitPCIeX4 {
  override lazy val module = new U500PolarFireEvalKitSystemModule(this)
}

class U500PolarFireEvalKitSystemModule[+L <: U500PolarFireEvalKitSystem](_outer: L)
  extends RocketCoreplexModule(_outer)
    with HasRTCModuleImp
    with HasPeripheryDebugModuleImp
    with HasPeripheryUARTModuleImp
    with HasPeripherySPIModuleImp
    with HasPeripheryGPIOModuleImp
//    with HasMemoryPolarFireEvalKitDDR4ModuleImp
    with HasMemoryPolarFireEvalKitDDR3ModuleImp
    with HasSystemPolarFireEvalKitPCIeX4ModuleImp {
  // Reset vector is set to the location of the mask rom
  val maskROMParams = p(PeripheryMaskROMKey)
//  global_reset_vector := maskROMParams(0).address.U
  global_reset_vector := BigInt(0x80000000L).U
}
