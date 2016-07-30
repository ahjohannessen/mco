package mco.core.standard

import better.files.File
import mco.core.general.{Media, Source}
import mco.core.monad.IO

final class FolderSource private (folder: File, media: File => IO[Option[Media]]) extends Source[IO] {
  override def add(f: File): IO[Source[IO]] = IO {
    f.copyTo(folder)
    new FolderSource(folder, media)
  }

  override def remove(s: String): IO[Source[IO]] = IO {
    val file = folder / s
    file.delete()
    new FolderSource(folder, media)
  }

  override lazy val list: IO[Map[String, Media]] = IO flat { io =>
    folder
      .children
      .flatMap(file => io(media(file)).map((file.name, _)))
      .toMap
  }
}

object FolderSource {
  def apply(f: File, medias: Media.Companion*) = IO {
    if (f.isDirectory) Some(new FolderSource(f, mediaGenerator(medias.toList)))
    else None
  }


  private def mediaGenerator(factories: List[Media.Companion]): File => IO[Option[Media]] =
    file => IO.flat { io =>
      factories
        .flatMap(factory => io(factory(file)))
        .headOption
    }
}