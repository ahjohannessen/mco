package mco.io

import mco.general._
import mco.io.Files._

object Classifiers {
  val customizable: Classifier[IO] = Classifier[IO](_ => ContentKind.Mod())
  val noncustomizable: Classifier[IO] = Classifier[IO](_ => ContentKind.Asset)
}