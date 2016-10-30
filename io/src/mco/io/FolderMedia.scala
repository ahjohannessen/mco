package mco.io

import scala.language.postfixOps

import mco.io.files._
import cats.syntax.option._
import cats.syntax.functor._
import cats.instances.vector._
import mco.Media

final class FolderMedia private (val path: Path) extends Media[IO] with NeighborFileThumbnail {
  override val key: String = path.fileName

  override def readContent: IO[Set[String]] =
    for (descendants <- descendantsOf(path)) yield {
      descendants map {_ relativeToS path} toSet
    }

  override def copy(m: Map[String, String]): IO[Unit] =
    IO.traverse(m.toVector){ case (from, to) => copyTree(path / from, Path(to)) }
      .void
}

object FolderMedia extends Media.Companion[IO] {
  override def apply(path: String): IO[Option[Media[IO]]] =
    for (isDir <- isDirectory(Path(path))) yield
      if (isDir) { new FolderMedia(Path(path)) }.some
      else none
}
