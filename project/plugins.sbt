logLevel := Level.Warn
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.2.0")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")