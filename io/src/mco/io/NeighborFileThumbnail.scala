package mco.io

import java.net.URL

import mco.Media
import mco.io.files._
import cats.implicits._

trait NeighborFileThumbnail { this: Media[IO] =>

  def path: Path

  final override def thumbnail: IO[Option[URL]] = {
    def urlIfExists(file: Path)(exists: Boolean) =
      if (exists) Some(file.toURL) else None

    def loadIfExists(picture: Path): IO[Option[URL]] =
      pathExists(picture) map urlIfExists(picture)

    ImageExtensions
      .map(ext => Path(s"${path.asString}.$ext"))
      .traverse(loadIfExists)
      .map(_.foldK)
  }

  final override def setThumbnail(location: String): IO[Unit] = {
    val fromPath = Path(location)

    for {
      isFile <- isRegularFile(fromPath)
      _ <- Fail.UnexpectedType(location, "existing file") when !isFile
      ext <- fromPath.extension
        .filter(ImageExtensions.contains)
        .map(IO.pure)
        .getOrElse(Fail.UnexpectedType(location, s"${ImageExtensions.mkString} image").io)
      _ <- discardThumbnail
      toPath = Path(s"${path.asString}.$ext")
      _ <- copyTree(fromPath, toPath)
    } yield ()
  }

  final override def discardThumbnail: IO[Unit] =
    ImageExtensions
      .map(ext => Path(s"${path.asString}.$ext"))
      .traverse_(removeIfExists)
}
