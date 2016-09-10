package mco.io

import mco.general.Media
import mco.io.Files.{IO, Path}
import org.scalacheck.Arbitrary

object Stubs {
  def media(mappings: (String, Set[String])*) = new Media.Companion[IO] {
    private val mappingMap = mappings.toMap

    class MediaStub(val key: String, cnt: Set[String]) extends Media[IO] {
      override def readContent: IO[Set[String]] = cnt.pure[IO]

      override def copy(content: Map[String, String]): IO[Unit] = sys.error("not implemented")
    }


    override def apply(v1: String): IO[Option[Media[IO]]] =
      mappingMap.get(v1).map(new MediaStub(Path(v1).fileName, _): Media[IO]).pure[IO]

  }

  val emptys: Set[String] = Set.empty

  def randoms: Set[String] = implicitly[Arbitrary[Set[String]]].arbitrary
    .filter(_.nonEmpty)
    .sample
    .get
}
