package mco.io

import java.net.URL

import scala.util.{Failure, Try}
import scala.concurrent.duration._

import cats.syntax.applicative._
import cats.syntax.functor._
import mco.{Package, Repository, Thumbnail, UnitSpec}
import mco.io.IOInterpreters.FSDsl._
import mco.io.files._

class BulkIOSpec extends UnitSpec {
  "BulkIO#classify" should "separate packages and images based on repo" in {
    val (_, BulkIO.ClassifiedPaths(pkgs, imgs)) = BulkIO.classify(
      Vector("1.file", "2.file", "img.png", "2.jpg") map Path
    )(repoStub) on fs(
      "1.file"  -> dir(),
      "2.file"  -> obj(),
      "2.jpg"   -> obj(),
      "img.png" -> obj()
    )

    pkgs should contain theSameElementsAs Set("1.file", "2.file").map(Path)
    imgs should contain theSameElementsAs Set("2.jpg", "img.png").map(Path)
  }

  it should "order separated files based on date created" in {
    val (_, BulkIO.ClassifiedPaths(pkgs, _)) = BulkIO.classify(
      Vector("1.file", "2.file") map Path
    )(repoStub) on fs(
      "1.file" -> obj(offset = 2.days),
      "2.file" -> obj(offset = 1.day)
    )

    pkgs should contain theSameElementsInOrderAs Seq("2.file", "1.file").map(Path)
  }

  it should "fail, reporting all files that could not be treated as packages or images" in {
    Try {
      BulkIO.classify(Vector(Path("bogus.jpg"), Path("bogus.pkg")))(repoStub) on fs(
        "bogus.jpg" -> dir(),
        "bogus.pkg" -> obj()
      )
    } should matchPattern {
      case Failure(
        Fail.MultipleFailures(
          Fail.UnexpectedType("bogus.jpg", _),
          Fail.UnexpectedType("bogus.pkg", _)
        )
      ) =>
    }
  }

  "BulkIO#add" should "add packages with mapped images, if any" in {
    val assoc = Map(
      Path("noImage.file")   -> None,
      Path("withImage.file") -> Some(Path("withImage.foo"))
    )
    val (state, _) = BulkIO.add(assoc)(repoStub) on fs(
      "repo"           -> dir(),
      "noImage.file"   -> obj(),
      "withImage.file" -> obj(),
      "withImage.foo"  -> obj()
    )

    deepGet(Path("repo"))(state) shouldBe Some(dir(
      "noImage.file"   -> obj(),
      "withImage.foo"  -> obj(),
      "withImage.file" -> obj()
    ))
  }

  private def repoStub = new Repository[IO, Unit] {
    override def key: String = fail()
    override def state: Unit = fail()
    override def apply(key: String): Package = fail()
    override def thumbnail(key: String): Thumbnail[IO] = new Thumbnail[IO] {
      override def url: IO[Option[URL]] = None.pure[IO].widen

      override def setFrom(location: String): IO[Unit] =
        for (_ <- copyTree(Path(location), Path("repo") / location)) yield ()

      override def discard: IO[Unit] = fail()
      override def reassociate(to: String): IO[Unit] = fail()
    }
    override def packages: Traversable[Package] = fail()
    override def change(oldKey: String, updates: Package): IO[Self] = fail()

    override def canAdd(f: String): IO[Boolean] =
      (f endsWith ".file").pure[IO]

    override def add(f: String): IO[(Package, Self)] =
      for (_ <- copyTree(Path(f), Path("repo") / f))
        yield (Package(f, Set()), this)

    override def remove(s: String): IO[Self] = fail()
  }
}
