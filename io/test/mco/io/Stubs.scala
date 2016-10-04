package mco.io

import mco.io.files._
import cats.syntax.applicative._
import cats.instances.vector._
import mco.{Media, Package, Source}

object Stubs {
  def media(mappings: (String, Set[String])*): Media.Companion[IO] = new Media.Companion[IO] {
    private val map = mappings.toMap

    class MediaStub(path: Path, cnt: Set[String]) extends Media[IO] {
      override val key: String = path.fileName

      override def readContent: IO[Set[String]] = cnt.pure[IO]

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

    override def add(f: String): IO[Source[IO]] =
      sys.error("Operation not supported")

    override def remove(s: String): IO[Source[IO]] =
      sys.error("Operation not supported")

    override def rename(from: String, to: String): IO[(Source[IO], Media[IO])] =
      sys.error("Operation not supported")
  }

  val emptys: Set[String] = Set.empty

  val randoms: Set[String] = Set("aa", "текст", "01", "夢")
}
