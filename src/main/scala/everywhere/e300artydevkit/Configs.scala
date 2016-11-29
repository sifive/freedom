// See LICENSE for license details.
package sifive.freedom.everywhere.e300artydevkit

import config._
import coreplex._
import rocketchip._


class DefaultFreedomEConfig extends Config(
  new WithStatelessBridge        ++
  new WithNBreakpoints(2)        ++
  new WithRV32                   ++
  new DefaultSmallConfig
)

class WithBootROMFile(bootROMFile: String) extends Config(
  (pname, site, here) => pname match {
    case BootROMFile => bootROMFile
    case _ => throw new CDEMatchError
  }
)

class E300ArtyDevKitConfig extends Config(
  new WithBootROMFile("./bootrom/e300artydevkit.img") ++
  new WithNExtTopInterrupts(0) ++
  new WithJtagDTM ++
  new WithL1ICacheSets(8192/32) ++ // 8 KiB **per set**
  new WithCacheBlockBytes(32) ++
  new WithL1ICacheWays(2) ++
  new WithDefaultBtb ++
  new WithFastMulDiv ++
  new WithDataScratchpad(16384) ++
  new WithNMemoryChannels(0) ++
  new WithoutFPU ++
  new WithTLMonitors ++
  new DefaultFreedomEConfig
)
