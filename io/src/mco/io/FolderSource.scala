package mco.io

import cats.data.OptionT
import cats.instances.stream._
import cats.instances.vector._
import cats.syntax.traverse._
import mco._
import mco.io.files._

final class FolderSource private (path: Path, media: Path => IO[Option[(Package, Media[IO])]]) extends Source[IO] {

  override def rename(from: String, to: String): IO[(Source[IO], Media[IO])] = for {
    conflict <- pathExists(path / to)
    _ <- Fail.NameConflict(to) when conflict
    _ <- moveTree(path / from, path / to)
    m <- media(path / to)
  } yield (new FolderSource(path, media), m.get._2)

  override def add(f: String): IO[Source[IO]] = for {
    exists <- pathExists(Path(f))
    _ <- Fail.MissingResource(f) when !exists
    conflicts <- pathExists(path / f)
    _ <- Fail.NameConflict(f) when conflicts
    _ <- copyTree(Path(f), path / f)
  } yield new FolderSource(path, media)

  override def remove(s: String): IO[Source[IO]] =
    for (_ <- removeFile(path / s)) yield new FolderSource(path, media)

  override val list: IO[Stream[(Package, Media[IO])]] = for {
    children <- childrenOf(path)
    rr <- children traverse media
  } yield rr.flatten
}

object FolderSource {
  def apply(f: String, classifier: Classifier[IO], medias: Media.Companion[IO]*): IO[Source[IO]] = for {
    isDir <- isDirectory(Path(f))
    _ <- Fail.UnexpectedType(f, "folder") when !isDir
  } yield new FolderSource(Path(f), p => mediaGenerator(medias.toVector, classifier, p.asString))


  private def mediaGenerator(factories: Vector[Media.Companion[IO]],
                             classifier: Classifier[IO],
                             path: String) =
      (for {
        media <- OptionT[IO, Media[IO]](IO.traverse(factories)(_(path)).map(_.flatten.headOption))
        pkg <- OptionT.liftF[IO, Package](classifier(media))
      } yield (pkg, media)).value
}