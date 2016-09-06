package mco.io

import java.nio.file.Paths

import mco.general.Media
import mco.io.Files._
import mco.utils.WhenOperator._

final class ArchiveMedia private (val path: Path) extends Media[Files.IO] {
  override val key = path.fileName

  override def readContent = archiveEntries(path)

  override def copy(map: Map[String, String]) = extract(path, map.mapValues(Path(_)))
}

object ArchiveMedia extends Media.Companion[IO] {
  val supportedExtensions = Set("7z", "zip", "rar") map { "." + _ }

  override def apply(path: String): IO[Option[Media[IO]]] = for {
    isFile <- Files.isRegularFile(Files.Path(path))
    extensionSupported = supportedExtensions exists { path.endsWith }
  } yield when (isFile && extensionSupported) { new ArchiveMedia(Path(path)): Media[IO] }
}