// See LICENSE for license details.
package sifive.freedom.unleashed.u500vc707devkit

import config._
import coreplex.{WithL1DCacheWays, WithSmallCores, WithoutFPU, BootROMFile}
import rocketchip.{BaseConfig,WithRTCPeriod,WithJtagDTM}

// Don't use directly. Requires additional bootfile configuration
class DefaultFreedomUConfig extends Config(
  new WithJtagDTM ++ new BaseConfig
)

class WithBootROMFile(bootROMFile: String) extends Config(
  (pname, site, here) => pname match {
    case BootROMFile => bootROMFile
    case _ => throw new CDEMatchError
  }
)

//----------------------------------------------------------------------------------
// Freedom U500 VC707 Dev Kit

class U500VC707DevKitConfig extends Config(
  new WithBootROMFile("./bootrom/u500vc707devkit.img") ++
  new WithRTCPeriod(62) ++    //Default value of 100 generates 1 Mhz clock @ 100Mhz, then corrected in sbi_entry.c
                              //Value 62 generates ~ 1Mhz clock @ 62.5Mhz
  new WithoutFPU ++
  new DefaultFreedomUConfig)
