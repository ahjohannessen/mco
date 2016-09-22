package mco.io

import mco.{Media, UnitSpec}
import mco.io.files.{IO, Path}
import IOInterpreters._
import IOInterpreters.FSDsl._

class ArchiveMediaSpec extends UnitSpec {
  "ArchiveMedia companion" should "create media for .7z, .rar and .zip archives" in {
    val (state, _) = pureTestMedia
    val media = List("7z", "rar", "zip")
      .map(ext => ArchiveMedia(s"seed.$ext"))
      .map(StubIORunner(state).value _)

    all (media) should not be empty
  }

  it should "not create media for folders or text files" in {
    val (state, _) = pureTestMedia
    val expectedEmpty = Seq("/regular.file", "/folder")
      .map(ArchiveMedia)
      .map(StubIORunner(state).value _)

    all (expectedEmpty) shouldBe empty
  }

  it should "not create media for nonexistent archive" in {
    val (state, _) = pureTestMedia
    StubIORunner(state).value(ArchiveMedia("/nope/does/not/exist.7z")) shouldBe empty
  }

  "ArchiveMedia#readContent" should "list archived files" in {
    val (state, media) = pureTestMedia
    StubIORunner(state).value(media.readContent) should contain theSameElementsAs someNames
  }

  "ArchiveMedia#readData" should "load archived file contents" in {
    val (state, media) = pureTestMedia
    val io = media.copy(Map("file2.txt" -> "ZZZZZZZZZ"))
    val result = deepGet(Path("ZZZZZZZZZ"))(StubIORunner(state).state(io))
    result shouldEqual Some(obj("file2.txt".getBytes))
  }

  private val someNames = (1 to 2) map (i => s"file$i.txt")
  private val someFiles = someNames map (f => f -> obj(f.getBytes))

  private def pureTestMedia: (Dir, Media[IO]) = {
    val testArchive = arc(someFiles: _*)
    val state = fs(
      "seed.zip" -> testArchive,
      "seed.rar" -> testArchive,
      "seed.7z" -> testArchive,
      "regular.file" -> obj(),
      "folder" -> dir(someFiles: _*)
    )
    val media = StubIORunner(state)
      .value(ArchiveMedia("seed.zip"))
      .getOrElse(fail("Could not create media"))

    (state, media)
  }
}
