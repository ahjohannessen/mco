package mco.io

import scala.language.postfixOps

import mco.general.Media
import mco.io.Files._
import mco.utils.WhenOperator._

final class FolderMedia private (val key: String) extends Media[IO] {
  val path = Path(key)

  override def readContent = for (descendants <- descendantsOf(path)) yield {
    descendants map {_ relativeTo path asString} toSet
  }

  override def copy(from: String)(to: String) = Files.copy(path / from, Path(to))
}

object FolderMedia extends Media.Companion[IO] {
  override def apply(path: String) =
    for (isDir <- isDirectory(Path(path))) yield when (isDir) { new FolderMedia(path) }
}
