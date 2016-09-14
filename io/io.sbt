resolvers += "jitpack" at "https://jitpack.io"
resolvers += Resolver.jcenterRepo

libraryDependencies ++=
  Seq(
    "org.typelevel" %% "alleycats-core" % "0.1.6",
    "com.thangiee" %% "freasy-monad" % "0.1.0",

    "com.typesafe" % "config" % "1.3.0",
    "com.github.pathikrit" %% "better-files" % "2.16.0",
    "org.slf4j" % "slf4j-simple" % "1.7.21",
    "com.github.oleg-py" %% "schive" % "-SNAPSHOT",
    "net.sf.sevenzipjbinding" % "sevenzipjbinding-all-windows" % "9.20-2.00beta"
  )

addCompilerPlugin("org.scalamacros" % "paradise_2.11.8" % "2.1.0")
