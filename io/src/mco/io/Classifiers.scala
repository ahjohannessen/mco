package mco.io

import mco.{Classifier, ContentKind}
import mco.io.files.IO

object Classifiers {
  val enableAll: Classifier[IO] = Classifier[IO](_ => ContentKind.Mod)
  val disableAll: Classifier[IO] = Classifier[IO](_ => ContentKind.Garbage)
}