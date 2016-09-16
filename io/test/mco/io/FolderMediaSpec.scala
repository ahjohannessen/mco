package mco.io

import better.files.File
import mco.{Media, UnitSpec}
import mco.io.files.{IO, Path, unsafePerformIO}
import IOInterpreters._
import IOInterpreters.FSDsl._

class FolderMediaSpec extends UnitSpec {
  "FolderMedia companion" should "create media for folders" in {
    val media = testMedia()
    media should not be empty
  }

  it should "not create media for regular files" in {
    val media = List("7z", "rar", "zip")
      .map(ext => getClass.getResource(s"/test_archive.$ext").toURI)
      .map(File(_).pathAsString)
      .map(FolderMedia)
      .map(unsafePerformIO)

    all (media) shouldBe empty
  }

  it should "not create media for nonexistend dir" in {
    unsafePerformIO(FolderMedia("/nope/does/not/exist")) shouldBe empty
  }

  val testFolder = File(getClass.getResource("/test_folder").toURI)
  def testMedia(): Option[Media[IO]] = unsafePerformIO(FolderMedia(testFolder.pathAsString))

  private def pureTestMedia: (Dir, Media[IO]) = {
    val state = fs("seed" -> dir("file1" -> obj(), "file2" -> obj()))
    val media = StubIORunner(state)
      .value(FolderMedia("seed"))
      .getOrElse(fail("Could not create media"))
    (state, media)
  }

  "FolderMedia#readContent" should "list contained files with their hashes" in {
    val media = testMedia() getOrElse fail("Could not create media")
    unsafePerformIO(media.readContent) shouldEqual Set("file1", "file2", "file3")
  }

  "FolderMedia#readData" should "load contained file contents" in {
    val (state, media) = pureTestMedia
    val op = media.copy(Map("file1" -> "fileZ"))
    val result = StubIORunner(state).state(op)
    deepGet(Path("fileZ"))(result) shouldBe Some(obj())
  }
}
