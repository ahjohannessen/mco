resolvers ++= Seq(
  "jitpack" at "https://jitpack.io",
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "alleycats-core" % "0.1.7",
  "com.thangiee" %% "freasy-monad" % "0.2.0",
  "com.chuusai" %% "shapeless" % "2.3.2",

  "com.propensive" %% "rapture-json-jawn" % "2.0.0-M7",


  "com.typesafe" % "config" % "1.3.0",
  "com.github.pathikrit" %% "better-files" % "2.16.0",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "com.github.oleg-py" %% "schive" % "-SNAPSHOT",
  "net.sf.sevenzipjbinding" % "sevenzipjbinding-all-platforms" % "9.20-2.00beta"
)