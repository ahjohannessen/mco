lazy val mco = project in file(".") aggregate (core, io) settings (commonSettings: _*)

lazy val core = project
  .settings(commonSettings: _*)

lazy val io = project
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings: _*)

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  crossPaths := false,

  sourceDirectory in Compile <<= baseDirectory(_ / "src"),
  scalaSource in Compile <<= baseDirectory(_ / "src"),
  javaSource in Compile <<= baseDirectory(_ / "src"),

  sourceDirectory in Test <<= baseDirectory(_ / "test"),
  scalaSource in Test     <<= baseDirectory(_ / "test"),
  javaSource in Test      <<= baseDirectory(_ / "test"),

  resourceDirectory in Compile <<= baseDirectory(_ / "resources"),
  resourceDirectory in Test    <<= baseDirectory(_ / "fixtures"),

  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.0"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),

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
)

val openReport = TaskKey[Unit]("open-report")
openReport := {
  java.awt.Desktop.getDesktop.browse(file("target/scoverage-report/index.html").toURI)
}

commands += Command.command("showCoverage") { state =>
  "clean" :: "coverage" :: "test" :: "coverageReport" :: "coverageAggregate" :: "openReport" :: state
}
