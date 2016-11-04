package mco.io

import cats.instances.option._
import cats.instances.stream._
import cats.syntax.all._
import mco._
import mco.Media.{Companion => MediaC}
import mco.io.files._

final class FolderSource private (backingFolder: Path,
                                  getMedia: Path => IO[Option[Media[IO]]],
                                  classifier: Classifier[IO])
  extends Source[IO]
{

  override val list: IO[Stream[(Package, Media[IO])]] =
    for {
      children <- childrenOf(backingFolder)
      medias   <- children traverseFilter getMedia
      packages <- medias traverse classifier
    } yield packages zip medias

  override def rename(from: String, to: String): IO[Media[IO]] = for {
    conflict   <- pathExists(backingFolder / to)
    _          <- Fail.NameConflict(to) when conflict
    oldMedia   <- getMedia(backingFolder / to).orDie
    _          <- oldMedia.thumbnail.reassociate(to)
    _          <- moveTree(backingFolder / from, backingFolder / to)
    media      <- getMedia(backingFolder / to).orDie
  } yield media

  override def add(key: String): IO[(Package, Media[IO])] = {
    val fromPath = Path(key)
    val toPath = backingFolder / fromPath.fileName

    for {
      exists     <- pathExists(fromPath)
      _          <- Fail.MissingResource(key) when !exists
      conflicts  <- pathExists(toPath)
      _          <- Fail.NameConflict(key) when conflicts
      _          <- copyTree(fromPath, toPath)

      media      <- getMedia(toPath).orRun(
        removeFile(toPath) followedBy
          Fail.UnexpectedType(key, "supported media").io)

      pkg        <- classifier(media)
    } yield (pkg, media)
  }

  override def remove(key: String): IO[Unit] = for {
      media <- getMedia(backingFolder / key) orFail Fail.MissingResource(key)
      _ <- media.thumbnail.discard
      _ <- removeFile(backingFolder / key)
    } yield ()
}

object FolderSource {
  def apply(dir: String, classifier: Classifier[IO], medias: MediaC[IO]*): IO[Source[IO]] =
    for {
      isDir <- isDirectory(Path(dir))
      _     <- Fail.UnexpectedType(dir, "folder") when !isDir
    } yield new FolderSource(Path(dir), mediasBy(medias.toStream), classifier)


  private def mediasBy(factories: Stream[MediaC[IO]])(p: Path) = {
    factories
      .traverse(_.apply(p.asString))
      .map(_.foldK)
  }
}
