resolvers ++= Seq(
  "Jitpack" at "https://jitpack.io",
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "alleycats-core" % "0.1.7",
  "com.thangiee" %% "freasy-monad" % "0.4.0",

  "com.propensive" %% "rapture-json-jawn" % "2.0.0-M7",

  "com.iheart" %% "ficus" % "1.2.6",

  "com.github.pathikrit" %% "better-files" % "2.16.0",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "com.github.oleg-py" %% "schive" % "0.1.0",
  "net.sf.sevenzipjbinding" % "sevenzipjbinding-all-platforms" % "9.20-2.00beta"
)
