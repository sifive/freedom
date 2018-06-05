// See LICENSE for license details.
package sifive.freedom.unleashed.u500vcu118devkit

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._

import sifive.fpgashells.devices.xilinx.xilinxvcu118mig._

//-------------------------------------------------------------------------
// U500VCU118DevKitSystem
//-------------------------------------------------------------------------

class U500VCU118DevKitSystem(implicit p: Parameters) extends RocketSubsystem
    with HasPeripheryMaskROMSlave
    with HasPeripheryDebug
    with HasSystemErrorSlave
    with HasPeripheryUART
    with HasPeripherySPI
    with HasPeripheryGPIO
    with HasMemoryXilinxVCU118MIG {
  override lazy val module = new U500VCU118DevKitSystemModule(this)
}

class U500VCU118DevKitSystemModule[+L <: U500VCU118DevKitSystem](_outer: L)
  extends RocketSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasPeripheryDebugModuleImp
    with HasPeripheryUARTModuleImp
    with HasPeripherySPIModuleImp
    with HasPeripheryGPIOModuleImp
    with HasMemoryXilinxVCU118MIGModuleImp {
  // Reset vector is set to the location of the mask rom
  val maskROMParams = p(PeripheryMaskROMKey)
  global_reset_vector := maskROMParams(0).address.U
}
