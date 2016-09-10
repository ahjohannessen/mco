import java.awt.Desktop

name := "mco"
version := "0.1"
scalaVersion := "2.11.8"

crossPaths := false

val src = baseDirectory(_ / "src")
sourceDirectory in Compile <<= src
scalaSource in Compile     <<= src
javaSource in Compile      <<= src

val testSrc = baseDirectory(_ / "test" / "src")
sourceDirectory in Test <<= testSrc
scalaSource in Test     <<= testSrc
javaSource in Test      <<= testSrc

resourceDirectory in Compile <<= baseDirectory(_ / "resources")
resourceDirectory in Test  <<= baseDirectory(_ / "test" / "resources")

resolvers += "jitpack" at "https://jitpack.io"
resolvers += Resolver.jcenterRepo

libraryDependencies ++=
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.2" % "test",

    "org.typelevel" %% "cats" % "0.7.2",
    "org.typelevel" %% "alleycats-core" % "0.1.6",
    "com.thangiee" %% "freasy-monad" % "0.1.0",

    "com.typesafe" % "config" % "1.3.0",
    "com.github.pathikrit" %% "better-files" % "2.16.0",
    "net.openhft" % "zero-allocation-hashing" % "0.6",
    "org.slf4j" % "slf4j-simple" % "1.7.21",
    "com.github.oleg-py" %% "schive" % "-SNAPSHOT",
    "net.sf.sevenzipjbinding" % "sevenzipjbinding-all-windows" % "9.20-2.00beta"
  )

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
//  "-Xfatal-warnings",
  "-Xlint",
  "-Xfuture",
  "-Ywarn-unused-import"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.11.8" % "2.1.0")

val openReport = TaskKey[Unit]("open-report")
openReport := {
  Desktop.getDesktop.browse(file("target/scoverage-report/index.html").toURI)
}

commands += Command.command("showCoverage") { state =>
  "clean" :: "coverage" :: "test" :: "coverageReport" :: "openReport" :: state
}