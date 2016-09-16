package mco.io

import better.files.File
import mco.{Media, UnitSpec}
import mco.io.files.{IO, Path, ops, unsafePerformIO}
import IOInterpreters._
import IOInterpreters.FSDsl._

class ArchiveMediaSpec extends UnitSpec {
  "ArchiveMedia companion" should "create media for .7z, .rar and .zip archives" in {
    val media = List("7z", "rar", "zip")
      .map(ext => getClass.getResource(s"/test_archive.$ext").toURI)
      .map(File(_).pathAsString)
      .map(ArchiveMedia)
      .map(unsafePerformIO)

    all (media) should not be empty
  }

  it should "not create media for folders or text files" in {
    val f1 = getClass.getResource(s"/test_folder")
    val f2 = getClass.getResource(s"/test_folder/file1")
    val expectedEmpty = Seq(f1, f2)
      .map(_.toURI)
      .map(File(_).pathAsString)
      .map(ArchiveMedia)
      .map(unsafePerformIO)

    all (expectedEmpty) shouldBe empty
  }

  it should "not create media for nonexistent archive" in {
    unsafePerformIO(ArchiveMedia("/nope/does/not/exist.7z")) shouldBe empty
  }

  val testArchive = File(getClass.getResource("/test_archive.rar").toURI)
  def testMedia(): Media[IO] = unsafePerformIO(ArchiveMedia(testArchive.pathAsString))
    .getOrElse(fail("Failed to create archive media"))

  def pureTestMedia: (Dir, Media[IO]) = {
    val state = fs("seed.zip" -> arc("file1" -> obj()))
    val media = StubIORunner(state)
      .value(ArchiveMedia("seed.zip"))
      .getOrElse(fail("Could not create media"))
    (state, media)
  }

  "ArchiveMedia#readContent" should "list archived files" in {
    val media = testMedia()
    unsafePerformIO(media.readContent) shouldEqual Set("file1", "file2", "file3")
  }

  "ArchiveMedia#readData" should "load archived file contents" in {
    val (state, media) = pureTestMedia
    val io = media.copy(Map("file1" -> "fileZ"))
    deepGet(Path("fileZ"))(StubIORunner(state).state(io)) shouldEqual Some(obj())
  }
}
