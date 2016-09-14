package mco.io

import mco.general.Media
import mco.io.files.Path
import mco.io.files.ops._
import org.scalacheck.Arbitrary
import cats.syntax.applicative._
import cats.instances.vector._

object Stubs {
  def media(mappings: (String, Set[String])*) = new Media.Companion[IO] {
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

  val emptys: Set[String] = Set.empty

  def randoms: Set[String] = implicitly[Arbitrary[Set[String]]].arbitrary
    .filter(_.nonEmpty)
    .sample
    .get
}
