package mco.io

import cats.implicits._

import mco.UnitSpec
import mco.io.files._
import mco.io.IOInterpreters.FSDsl._

class ImageThumbnailSpec extends UnitSpec {
  "ImageThumbnail#url" should "read image from is given filename plus image extension" in {
    val tests = Seq(
      "withNone" -> none[String],
      "withJpg.x" -> "jpg".some,
      "withJpeg.y" -> "jpeg".some,
      "withPng.z" -> "png".some
    )

    for ((name, urlEnd) <- tests) {
      val expectedImageName = urlEnd map (name + '.' + _)
      val urlIO = new ImageThumbnail(Path(name)).url
      (stub.value(urlIO), expectedImageName) match {
        case (None, None) => ()
        case (Some(url), Some(suffix)) => url.toString should endWith (suffix)
        case other @ (_, _) => fail(s"Expected same option types, got $other")
      }
    }
  }

  it should "prefer PNG images to JPG" in {
    val url = new ImageThumbnail(Path("withTwo")).url
    stub.value(url) map (_.toString takeRight 4) shouldBe Some(".png")
  }

  "ImageThumbnail#discard" should "remove all adjacent images" in {
    val discardIO = new ImageThumbnail(Path("withTwo")).discard
    val nextState = stub.state(discardIO)
    deepGet(Path("withTwo.png"))(nextState) shouldBe empty
    deepGet(Path("withTwo.jpg"))(nextState) shouldBe empty
  }

  "ImageThumbnail#setFrom" should "rename thumbnail if it is a valid image" in {
    val setIO = new ImageThumbnail(Path("foome")) setFrom "withPng.z.png"
    val nextState = stub.state(setIO)
    deepGet(Path("foome.png"))(nextState) should not be empty
    deepGet(Path("withPng.z.png"))(nextState) should not be empty
  }

  it should "fail if path leads to a directory or does not exist" in {
    val io = new ImageThumbnail(Path("justMe")) setFrom "notExisting.jpg"
    a [Fail.UnexpectedType] shouldBe thrownBy {
      stub(io)
    }

    val io2 = new ImageThumbnail(Path("test")) setFrom "bogus.png"
    a [Fail.UnexpectedType] shouldBe thrownBy {
      stub(io2)
    }
  }

  it should "fail if extension is not JPG, JPEG or PNG" in {
    val io = new ImageThumbnail(Path("test")) setFrom "bogus.foo"

    a [Fail.UnexpectedType] shouldBe thrownBy {
      stub(io)
    }
  }

  "ImageThumbnail#reassociate" should "rename the adjacent image files" in {
    val io = new ImageThumbnail(Path("withTwo")) reassociate "withTwoRE"

    val state = stub.state(io)

    deepGet(Path("withTwo"))(state) should not be empty
    deepGet(Path("withTwoRE"))(state) shouldBe empty
    deepGet(Path("withTwo.png"))(state) shouldBe empty
    deepGet(Path("withTwoRE.png"))(state) should not be empty
    deepGet(Path("withTwo.jpg"))(state) shouldBe empty
    deepGet(Path("withTwoRE.jpg"))(state) should not be empty
  }

  "ImageThumbnail.Provied" should "mixin ImageThumbnail as thumbnail implementation" in {
    new ImageThumbnail.Provided {
      override def path: Path = Path("foo")
    }.thumbnail shouldBe an [ImageThumbnail]
  }

  private def stub = StubIORunner(fs(
    "withNone" -> obj(),
    "withJpg.x" -> obj(),
    "withJpg.x.jpg" -> obj(),
    "withJpeg.y" -> obj(),
    "withJpeg.y.jpeg" -> obj(),
    "withPng.z" -> obj(),
    "withPng.z.png" -> obj(),
    "withTwo" -> dir(),
    "withTwo.png" -> obj(),
    "withTwo.jpg" -> obj(),
    "bogus.png" -> dir(),
    "bogus.foo" -> obj()
  ))
}
