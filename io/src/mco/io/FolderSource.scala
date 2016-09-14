package mco.io

import cats.data.{OptionT, Xor}
import cats.syntax.xor._
import cats.syntax.traverse._
import cats.instances.stream._
import cats.instances.vector._
import mco._
import mco.Media.Companion
import mco.io.files.Path
import mco.io.files.ops._
import mco.utils.WhenOperator._

final class FolderSource private (path: Path, media: Path => IO[Option[(mco.Package, Media[IO])]]) extends Source[IO] {

  override def rename(from: String, to: String): IO[Source[IO]] =
    for (_ <- moveTree(path / from, path / to)) yield new FolderSource(path, media)

  override def add(f: String): IO[String Xor Source[IO]] =
    for (_ <- copyTree(Path(f), path / f)) yield new FolderSource(path, media).right

  override def remove(s: String): IO[String Xor Source[IO]] =
    for (_ <- removeFile(path / s)) yield new FolderSource(path, media).right

  override val list: IO[Stream[(mco.Package, Media[IO])]] = for {
    children <- childrenOf(path)
    rr <- children traverse media
  } yield rr.flatten
}

object FolderSource {
  def apply(f: String, classifier: Classifier[IO], medias: Media.Companion[IO]*): IO[Option[FolderSource]] = for {
    isDir <- isDirectory(Path(f))
  } yield when (isDir) {
    new FolderSource(
      Path(f),
      p => mediaGenerator(medias.toVector, classifier, p.asString).value
    )
  }

  private def mediaGenerator(factories: Vector[Companion[IO]],
                             classifier: Classifier[IO],
                             path: String) =
      for {
        media <- OptionT[IO, Media[IO]](IO.traverse(factories)(_(path)).map(_.flatten.headOption))
        pkg <- OptionT.liftF[IO, mco.Package](classifier(media))
      } yield (pkg, media)
}