name := "mco"
version := "0.1"
scalaOrganization := "org.typelevel"
scalaVersion := "2.11.8"


crossPaths := false

val src = baseDirectory(_ / "src")
sourceDirectory in Compile <<= src
scalaSource in Compile     <<= src
javaSource in Compile      <<= src

val testSrc = baseDirectory(_ / "test" / "srcZZ")
sourceDirectory in Test <<= testSrc
scalaSource in Test     <<= testSrc
javaSource in Test      <<= testSrc

resourceDirectory in Compile <<= baseDirectory(_ / "resources")
resourceDirectory in Test  <<= baseDirectory(_ / "test" / "resources")

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++=
  Seq("org.scalatest" %% "scalatest" % "3.0.0" % "test"
    , "org.scalacheck" %% "scalacheck" % "1.13.2" % "test"

    , "org.typelevel" %% "cats" % "0.7.2"
    , "org.typelevel" %% "alleycats-core" % "0.1.6"
    , "com.typesafe" % "config" % "1.3.0"
    , "com.github.pathikrit" %% "better-files" % "2.16.0"
    , "net.openhft" % "zero-allocation-hashing" % "0.6"
    , "org.slf4j" % "slf4j-simple" % "1.7.21"
    , "com.github.oleg-py" %% "schive" % "-SNAPSHOT"
    , "net.sf.sevenzipjbinding" % "sevenzipjbinding-all-windows" % "9.20-2.00beta"
  )

scalacOptions ++= Seq("-feature", "-deprecation", "-Y")
