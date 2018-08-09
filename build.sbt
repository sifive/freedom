// See LICENSE for license details.
organization := "com.sifive"
name := "freedom"
version := "0.1.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",  // This needs to match rocket-chip's scalaVersion
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xsource:2.11",
    "-language:reflectiveCalls"
  )
)

// A RootProject (not well-documented) tells sbt to treat the target directory
// as its own root project, reading its build settings. If we instead used the
// normal `project in file()` declaration, sbt would ignore all of rocket-chip's
// build settings, and therefore not understand that it has its own dependencies
// on chisel, etc.
lazy val rocketChip = RootProject(file("rocket-chip"))

lazy val sifiveBlocks = (project in file("sifive-blocks")).
  dependsOn(rocketChip).
  settings(commonSettings: _*)

lazy val nvdlaBlocks = (project in file("nvidia-dla-blocks")).
  dependsOn(rocketChip).
  settings(commonSettings: _*)

lazy val fpgaShells = (project in file("fpga-shells")).
  dependsOn(rocketChip, sifiveBlocks).
  settings(commonSettings: _*)

lazy val freedomPlatforms = (project in file(".")).
  dependsOn(rocketChip, sifiveBlocks, nvdlaBlocks, fpgaShells).
  settings(commonSettings: _*)
