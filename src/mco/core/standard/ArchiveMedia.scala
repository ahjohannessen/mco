package mco.core.standard

import better.files.File
import com.olegpy.schive.Archive
import mco.core.general.{Content, ContentData, Media}
import mco.core.monad.IO

final class ArchiveMedia private (file: File) extends Media {
  override def readContent: IO[Set[Content]] = IO {
    Archive(file.path)
      .mapSome(e => Some((bytes: Array[Byte]) => Content(e.path, Utils.hashBytes(bytes))))
      .toSet
  }

  override def readData(c: Content): IO[ContentData] = IO {
    Archive(file.path)
      .mapSome(e => {
        if (e.path == c.key) Some((bytes: Array[Byte]) => new ArrayContentData(c, bytes))
        else None
      }).head
  }
}

object ArchiveMedia extends Media.Companion {
  val supportedExtensions = Set("7z", "zip", "rar")

  override def apply(file: File): IO[Option[ArchiveMedia]] = IO {
    for {
      ext <- file.extension(includeDot = false)
      if file.isRegularFile
      if supportedExtensions contains ext
    } yield new ArchiveMedia(file)
  }
}