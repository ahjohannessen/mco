package mco
package core
package standard

import better.files.File
import mco.core.general.{Content, Hash}

class ArchiveMediaSpec extends UnitSpec {
  "ArchiveMedia companion" should "create media for .7z, .rar and .zip archives" in {
    val media = List("7z", "rar", "zip")
      .map(ext => getClass.getResource(s"/test_archive.$ext").toURI)
      .map(File(_))
      .map(ArchiveMedia(_).io())

    all (media) should not be empty
  }

  it should "not create media for folders or text files" in {
    val f1 = getClass.getResource(s"/test_folder")
    val f2 = getClass.getResource(s"/test_folder/file1")
    val expectedEmpty = Seq(f1, f2)
      .map(_.toURI)
      .map(File(_))
      .map(ArchiveMedia(_).io())

    all (expectedEmpty) shouldBe empty
  }

  it should "not create media for nonexistent archive" in {
    FolderMedia(File("/nope/does/not/exist.7z")).io() shouldBe empty
  }

  def testMedia() = ArchiveMedia(File(getClass.getResource("/test_archive.rar").toURI)).io().get

  "ArchiveMedia#readContent" should "list archived files with their hashes" in {
    val media = testMedia()
    media.readContent.io() shouldEqual Set(
      Content("file1", Hash(753694413698530628L, -3042045079152025465L)),
      Content("file2", Hash(1846988464401551951L,-138626083409739144L)),
      Content("file3", Hash(-1878249218591150391L,4344127366426008665L))
    )
  }

  "ArchiveMedia#readData" should "load archived file contents" in {
    val media = testMedia()
    val content = Content("file1", Hash(753694413698530628L, -3042045079152025465L))
    val temp = File.newTemporaryFile()
    val contentData = media.readData(content).io()
    contentData.writeTo(temp).io()
    temp.contentAsString shouldEqual "Hello"
    temp.delete(swallowIOExceptions = true)
  }
}
