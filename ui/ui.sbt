libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "2.0.3",
  "org.scalafx" %% "scalafx" % "8.0.102-R11"
)

fork := true // Avoid problems with JavaFX double initalization
