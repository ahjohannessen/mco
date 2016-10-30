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
}
