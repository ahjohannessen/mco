package mco.io

import scala.language.postfixOps

import mco.general.Media
import mco.io.Files._
import mco.utils.WhenOperator._

final class FolderMedia private (val path: Path) extends Media[IO] {
  override val key = path.fileName

  override def readContent = for (descendants <- descendantsOf(path)) yield {
    descendants map {_ relativeToS path} toSet
  }

  override def copy(m: Map[String, String]) =
    IO
      .sequence(m.map{case (from, to) => Files.copy(path / from, Path(to))}.toVector)
      .map {_ => ()}
}

object FolderMedia extends Media.Companion[IO] {
  override def apply(path: String) =
    for (isDir <- isDirectory(Path(path))) yield when (isDir) { new FolderMedia(Path(path)) }
}
