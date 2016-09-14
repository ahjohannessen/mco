package mco.io

import better.files.File
import mco.{Media, UnitSpec}
import mco.io.files.{IO, Path, ops, unsafePerformIO}
import IOInterpreters._

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

  "FolderMedia#readContent" should "list contained files with their hashes" in {
    val media = testMedia().get
    unsafePerformIO(media.readContent) shouldEqual Set("file1", "file2", "file3")
  }

  "FolderMedia#readData" should "load contained file contents" in {
    val media = testMedia().get
    val op = media.copy(Map("file1" -> "fileZ"))
    lastOperation(op) shouldBe ops.OperationsADT.CopyTree(Path(testFolder / "file1"), Path("fileZ"))
  }
}
