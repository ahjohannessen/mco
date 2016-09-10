package mco.io

import scala.language.postfixOps

import mco.general.Media
import mco.io.files.Path
import mco.io.files.ops._
import mco.utils.WhenOperator._
import cats.instances.vector._

final class FolderMedia private (val path: Path) extends Media[IO] {
  override val key: String = path.fileName

  override def readContent: IO[Set[String]] =
    for (descendants <- descendantsOf(path)) yield {
      descendants map {_ relativeToS path} toSet
    }

  override def copy(m: Map[String, String]): IO[Unit] =
    IO.traverse(m.toVector){ case (from, to) => copyTree(path / from, Path(to)) }
      .map {_ => ()}
}

object FolderMedia extends Media.Companion[IO] {
  override def apply(path: String): IO[Option[Media[IO]]] =
    for (isDir <- isDirectory(Path(path))) yield when (isDir) { new FolderMedia(Path(path)) }
}
