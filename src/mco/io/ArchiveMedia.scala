package mco.io

import java.nio.file.Paths

import mco.general.Media
import mco.io.Files._
import mco.utils.WhenOperator._

final class ArchiveMedia private (val key: String) extends Media[Files.IO] {
  val path = Path(key)

  override def readContent = asArchive(path) map {_.files.map(_.path).toSet}

  override def copy(key: String)(target: String) =
    for (arch <- asArchive(path))
      yield arch.extractSome(e => when (e.path == key) { Paths.get(target) })
}

object ArchiveMedia extends Media.Companion[IO] {
  val supportedExtensions = Set("7z", "zip", "rar") map { "." + _ }

  override def apply(path: String): IO[Option[Media[IO]]] = for {
    isFile <- Files.isRegularFile(Files.Path(path))
    extensionSupported = supportedExtensions exists { path.endsWith }
  } yield when (isFile && extensionSupported) { new ArchiveMedia(path): Media[IO] }
}