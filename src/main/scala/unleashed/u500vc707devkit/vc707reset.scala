// See LICENSE for license details.
package sifive.freedom.unleashed.u500vc707devkit

import Chisel._

//scalastyle:off
//turn off linter: blackbox name must match verilog module 
class vc707reset() extends BlackBox
{
  val io = new Bundle{
    val areset = Bool(INPUT)
    val clock1 = Clock(INPUT)
    val reset1 = Bool(OUTPUT)
    val clock2 = Clock(INPUT)
    val reset2 = Bool(OUTPUT)
    val clock3 = Clock(INPUT)
    val reset3 = Bool(OUTPUT)
    val clock4 = Clock(INPUT)
    val reset4 = Bool(OUTPUT)
  }
}
//scalastyle:on
