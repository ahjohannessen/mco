package mco
package core
package standard

import better.files.File
import general.{Content, Hash}

class FolderMediaSpec extends UnitSpec {
  "FolderMedia companion" should "create media for folders" in {
    val media = testMedia()
    media should not be empty
  }

  it should "not create media for regular files" in {
    val media = List("7z", "rar", "zip")
      .map(ext => getClass.getResource(s"/test_archive.$ext").toURI)
      .map(File(_))
      .map(FolderMedia(_).io())

    all (media) shouldBe empty
  }

  it should "not create media for nonexistend dir" in {
    FolderMedia(File("/nope/does/not/exist")).io() shouldBe empty
  }

  def testMedia() = FolderMedia(File(getClass.getResource("/test_folder").toURI)).io()

  "FolderMedia#readContent" should "list contained files with their hashes" in {
    val media = testMedia().get
    media.readContent.io() shouldEqual Set(
      Content("file1", Hash(753694413698530628L, -3042045079152025465L)),
      Content("file2", Hash(1846988464401551951L,-138626083409739144L)),
      Content("file3", Hash(-1878249218591150391L,4344127366426008665L))
    )
  }

  "FolderMedia#readData" should "load contained file contents" in {
    val media = testMedia().get
    val content = Content("file1", Hash(753694413698530628L, -3042045079152025465L))
    val temp = File.newTemporaryFile()
    val contentData = media.readData(content).io()
    contentData.writeTo(temp).io()
    temp.contentAsString shouldEqual "Hello"
    temp.delete(swallowIOExceptions = true)
  }
}
