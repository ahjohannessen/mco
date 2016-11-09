package mco.io

import java.net.URL

import mco.io.files._
import cats.syntax.applicative._
import cats.syntax.option._
import cats.instances.vector._
import mco.{Media, Package, Source, Thumbnail}

object Stubs {
  object NoThumbnail extends Thumbnail[IO] {
    override def url: IO[Option[URL]] = none[URL].pure[IO]
    override def setFrom(location: String): IO[Unit] = ().pure[IO]
    override def discard: IO[Unit] = ().pure[IO]
    override def reassociate(to: String): IO[Unit] = ().pure[IO]
  }

  def media(mappings: (String, Set[String])*): Media.Companion[IO] = new Media.Companion[IO] {
    private val map = mappings.toMap

    class MediaStub(path: Path, cnt: Set[String]) extends Media[IO] {

      override def thumbnail: Thumbnail[IO] = NoThumbnail

      override val key: String = path.fileName

      override def contentKeys: IO[Set[String]] = cnt.pure[IO]

      override def copy(content: Map[String, String]): IO[Unit] =
        IO.traverse(content.toVector){
            case (from, to) if cnt contains from => setContent(Path(to), Array.emptyByteArray)
        } map (_ => ())
    }

    override def apply(v1: String): IO[Option[Media[IO]]] =
      map.get(v1).map(new MediaStub(Path(v1), _): Media[IO]).pure[IO]
  }

  def emptySource: Source[IO] = new Source[IO] {
    override def list: IO[Stream[(Package, Media[IO])]] = IO.pure(Stream())

    override def add(f: String): IO[(Package, Media[IO])] =
      sys.error("Operation not supported")

    override def remove(s: String): IO[Unit] =
      sys.error("Operation not supported")

    override def rename(from: String, to: String): IO[Media[IO]] =
      sys.error("Operation not supported")

    /**
      * Check if object described by key can be, in principle, added to the source as
      * a package. Does not need to check for conflicts, but should generally not fail.
      */
    override def canAdd(key: String): IO[Boolean] = IO.pure(false)
  }

  val emptys: Set[String] = Set.empty

  val randoms: Set[String] = Set("aa", "текст", "01", "夢")
}
