package mco.io

import cats.data.{OptionT, Xor}
import cats.instances.stream._
import cats.instances.vector._
import cats.syntax.option._
import cats.syntax.traverse._
import cats.syntax.xor._
import mco.Media.Companion
import mco._
import mco.io.files.Path
import mco.io.files.ops._

final class FolderSource private (path: Path, media: Path => IO[Option[(Package, Media[IO])]]) extends Source[IO] {

  override def rename(from: String, to: String): IO[Source[IO]] =
    for (_ <- moveTree(path / from, path / to)) yield new FolderSource(path, media)

  override def add(f: String): IO[Fail Xor Source[IO]] =
    for (_ <- copyTree(Path(f), path / f)) yield new FolderSource(path, media).right

  override def remove(s: String): IO[Fail Xor Source[IO]] =
    for (_ <- removeFile(path / s)) yield new FolderSource(path, media).right

  override val list: IO[Stream[(Package, Media[IO])]] = for {
    children <- childrenOf(path)
    rr <- children traverse media
  } yield rr.flatten
}

object FolderSource {
  def apply(f: String, classifier: Classifier[IO], medias: Media.Companion[IO]*): IO[Option[FolderSource]] = for {
    isDir <- isDirectory(Path(f))
  } yield
    if (isDir) new FolderSource(
      Path(f),
      p => mediaGenerator(medias.toVector, classifier, p.asString).value
    ).some else none

  private def mediaGenerator(factories: Vector[Companion[IO]],
                             classifier: Classifier[IO],
                             path: String) =
      for {
        media <- OptionT[IO, Media[IO]](IO.traverse(factories)(_(path)).map(_.flatten.headOption))
        pkg <- OptionT.liftF[IO, Package](classifier(media))
      } yield (pkg, media)
}