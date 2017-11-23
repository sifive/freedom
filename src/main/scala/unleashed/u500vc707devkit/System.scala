// See LICENSE for license details.
package sifive.freedom.unleashed.u500vc707devkit

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

import sifive.fpgashells.devices.xilinx.xilinxvc707mig._
import sifive.fpgashells.devices.xilinx.xilinxvc707pciex1._

//-------------------------------------------------------------------------
// U500VC707DevKitSystem
//-------------------------------------------------------------------------

class U500VC707DevKitSystem(implicit p: Parameters) extends RocketCoreplex
    with HasPeripheryMaskROMSlave
    with HasPeripheryDebug
    with HasSystemErrorSlave
    with HasPeripheryUART
    with HasPeripherySPI
    with HasPeripheryGPIO
    with HasMemoryXilinxVC707MIG {
  override lazy val module = new U500VC707DevKitSystemModule(this)
}

class U500VC707DevKitSystemModule[+L <: U500VC707DevKitSystem](_outer: L)
  extends RocketCoreplexModule(_outer)
    with HasRTCModuleImp
    with HasPeripheryDebugModuleImp
    with HasPeripheryUARTModuleImp
    with HasPeripherySPIModuleImp
    with HasPeripheryGPIOModuleImp
    with HasMemoryXilinxVC707MIGModuleImp {
  // Reset vector is set to the location of the mask rom
  val maskROMParams = p(PeripheryMaskROMKey)
  global_reset_vector := maskROMParams(0).address.U
}
