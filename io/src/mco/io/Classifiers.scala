package mco.io

import mco._
import mco.io.files.ops._

object Classifiers {
  val enableAll: Classifier[IO] = Classifier[IO](_ => ContentKind.Mod)
  val disableAll: Classifier[IO] = Classifier[IO](_ => ContentKind.Garbage)
}