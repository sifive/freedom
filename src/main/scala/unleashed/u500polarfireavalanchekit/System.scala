// See LICENSE for license details.
package sifive.freedom.unleashed.u500polarfireavalanchekit

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.uart._

import sifive.fpgashells.devices.microsemi.polarfireddr3._


//-------------------------------------------------------------------------
// U500PolarFireEvalKitSystem
//-------------------------------------------------------------------------

//class U500PolarFireAvalancheKitSystem(implicit p: Parameters) extends RocketCoreplex
class U500PolarFireAvalancheKitSystem(implicit p: Parameters) extends RocketSubsystem
    with HasPeripheryDebug
    with HasPeripheryUART
    with HasPeripheryGPIO
    with HasMemoryPolarFireEvalKitDDR3{
  override lazy val module = new U500PolarFireAvalancheKitSystemModule(this)
}

class U500PolarFireAvalancheKitSystemModule[+L <: U500PolarFireAvalancheKitSystem](_outer: L)
  extends RocketSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasPeripheryDebugModuleImp
    with HasPeripheryUARTModuleImp
    with HasPeripheryGPIOModuleImp
    with HasMemoryPolarFireEvalKitDDR3ModuleImp
    //with HasSystemPolarFireEvalKitPCIeX4ModuleImp
    {
  // Reset vector is set to the location of the mask rom
  //val maskROMParams = p(PeripheryMaskROMKey)
//  global_reset_vector := maskROMParams(0).address.U
  global_reset_vector := BigInt(0x80000000L).U
}
