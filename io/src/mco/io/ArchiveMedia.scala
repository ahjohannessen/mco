package mco.io

import mco.Media
import mco.io.files.Path
import mco.io.files.ops._
import mco.utils.WhenOperator._

final class ArchiveMedia private (val path: Path) extends Media[IO] {
  override val key: String = path.fileName
  override def readContent: IO[Set[String]] = archiveEntries(path)
  override def copy(map: Map[String, String]): IO[Unit] = extract(path, map.mapValues(Path(_)))
}

object ArchiveMedia extends Media.Companion[IO] {
  private val supportedExtensions = Set("7z", "zip", "rar") map { "." + _ }

  override def apply(path: String): IO[Option[Media[IO]]] = for {
    isFile <- isRegularFile(Path(path))
    extensionSupported = supportedExtensions exists { path.endsWith }
  } yield when (isFile && extensionSupported) { new ArchiveMedia(Path(path)): Media[IO] }
}