import mill._
import mill.scalalib._
import ammonite.ops._

import $file.^.`scala-wake`.common, common._

trait FreedomBase extends SbtModule with WakeModule with CommonOptions {
  def millSourcePath = os.pwd
}
