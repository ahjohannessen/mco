package mco.io

import java.net.URL

import cats.implicits._
import mco.Thumbnail
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

  override def setFrom(location: String): IO[Unit] = {
    val fromPath = Path(location)

    for {
      isFile <- isRegularFile(fromPath)
      _ <- Fail.UnexpectedType(location, "existing file") when !isFile
      ext <- fromPath.extension
        .filter(ImageExtensions.contains)
        .pure[IO]
        .orFail(Fail.UnexpectedType(location, s"${ImageExtensions mkString ", "} image"))
      _ <- discard
      toPath = Path(s"${path.asString}.$ext")
      _ <- copyTree(fromPath, toPath)
    } yield ()
  }

  override def discard: IO[Unit] =
    ImageExtensions
      .map(ext => Path(s"${path.asString}.$ext"))
      .traverse_(removeIfExists)

  override def reassociate(to: String): IO[Unit] = {
    def moveIfExists(from: Path, to: Path) = pathExists(from) flatMap
      { if (_) moveTree(from, to) else ().pure[IO] }
    val parent = path.parent
    ImageExtensions
      .map(ext => (Path(s"${path.asString}.$ext"), parent / s"$to.$ext"))
      .traverse_[IO, Unit]({ moveIfExists _ }.tupled)
  }
}

object ImageThumbnail {
  trait Provided {
    def path: Path
    final def thumbnail: ImageThumbnail = new ImageThumbnail(path)
  }
}
