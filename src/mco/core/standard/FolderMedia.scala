package mco.core.standard

import better.files.File
import mco.core.general.{Content, ContentData, Media}
import mco.core.monad.IO

final class FolderMedia private (path: File) extends Media {
  override def readContent: IO[Set[Content]] = IO.flat { perform =>
    path
      .listRecursively
      .map { f => Content(path.relativize(f).toString, perform(Utils.hashFile(f))) }
      .toSet
  }

  override def readData(c: Content): IO[ContentData] = IO {
    new ArrayContentData(c, (path / c.key.repr).byteArray)
  }
}

object FolderMedia extends Media.Companion {
  override def apply(file: File): IO[Option[FolderMedia]] = IO {
    if (file.isDirectory) Some(new FolderMedia(file))
    else None
  }
}
