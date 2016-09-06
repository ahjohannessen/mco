package mco.io

import mco.general._
import mco.io.Files._

object SimpleClassifier extends Classifier[IO] {
  override def apply(media: Media[IO]) = for (m <- media.readContent) yield
    Package(
      media.key,
      m.map(key => Content(key, ContentKind.Mod())),
      isInstalled = false
    )
}