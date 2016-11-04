package mco.io

import java.net.URL

import cats.implicits._
import mco.{Media, Thumbnail}
import mco.io.files._

final class ImageThumbnail (val path: Path) extends Thumbnail[IO] {

  override def url: IO[Option[URL]] = {
    def urlIfExists(file: Path)(exists: Boolean) =
      if (exists) Some(file.toURL) else None

    def loadIfExists(picture: Path): IO[Option[URL]] =
      pathExists(picture) map urlIfExists(picture)

    ImageExtensions
      .map(ext => Path(s"${path.asString}.$ext"))
      .traverse(loadIfExists)
      .map(_.foldK)
  }

  override def setThumbnail(location: String): IO[Unit] = {
    val fromPath = Path(location)

    for {
      isFile <- isRegularFile(fromPath)
      _ <- Fail.UnexpectedType(location, "existing file") when !isFile
      ext <- fromPath.extension
        .filter(ImageExtensions.contains)
        .orFail(Fail.UnexpectedType(location, s"${ImageExtensions.mkString} image"))
      _ <- discardThumbnail
      toPath = Path(s"${path.asString}.$ext")
      _ <- copyTree(fromPath, toPath)
    } yield ()
  }

  override def discardThumbnail: IO[Unit] =
    ImageExtensions
      .map(ext => Path(s"${path.asString}.$ext"))
      .traverse_(removeIfExists)

  override def reassociate(from: String, to: String): IO[Unit] = {
    ImageExtensions
      .map(ext => (Path(s"$from.$ext"), Path(s"$to.$ext")))
      .traverse_[IO, Unit](t => moveTree(t._1, t._2))
  }
}

object ImageThumbnail {
  trait Provided { this: Media[IO] =>
    def path: Path
    final override def thumbnail: ImageThumbnail = new ImageThumbnail(path)
  }
}
