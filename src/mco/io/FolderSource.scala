package mco.io

import cats.data.OptionT
import mco.general.{Classifier, Media, Package, Source}
import mco.io.Files._
import mco.utils.WhenOperator._

final class FolderSource private (path: Path, media: Path => IO[Option[(Package, Media[IO])]]) extends Source[IO] {

  override def rename(from: String, to: String): IO[Source[IO]] =
    for (_ <- move(path / from, path / to)) yield new FolderSource(path, media)

  override def add(f: String) =
    for (_ <- copy(Path(f), path / f)) yield new FolderSource(path, media).right

  override def remove(s: String) =
    for (_ <- removeFile(path / s)) yield new FolderSource(path, media).right

  override val list = for {
    children <- childrenOf(path)
    rr <- IO.traverse(children)(media)
  } yield rr.flatten
}

object FolderSource {
  def apply(f: String, classifier: Classifier[IO], medias: Media.Companion[IO]*): IO[Option[FolderSource]] = for {
    isDir <- isDirectory(Path(f))
  } yield when (isDir) {
    new FolderSource(
      Path(f),
      { (_: Path).asString } andThen mediaGenerator(medias.toVector, classifier)
    )
  }

  private def mediaGenerator(factories: Vector[Media.Companion[IO]], classifier: Classifier[IO])(path: String) = {
    for {
      media <- OptionT[IO, Media[IO]](
        IO.sequence(factories.map(_(path))).map(_.flatten.headOption))
      pkg <- OptionT.liftF[IO, Package](classifier(media))
    } yield (pkg, media)
  }.value

}