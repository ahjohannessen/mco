package mco.io

import mco.Media
import mco.io.files.Path
import mco.io.files.ops._
import cats.syntax.option._


final class FileMedia (val path: Path) extends Media[IO]{
  override val key: String = path.fileName

  override def readContent: IO[Set[String]] = IO.pure(Set(key))

  override def copy(content: Map[String, String]): IO[Unit] =
    content.get(key).map(p => copyTree(path, Path(p))).getOrElse(IO.pure(()))
}

object FileMedia extends Media.Companion[IO] {
  override def apply(path: String): IO[Option[Media[IO]]] = {
    isRegularFile(Path(path)).map(isFile =>
    if(isFile) { new FileMedia(Path(path))}.some
    else none)
  }
}