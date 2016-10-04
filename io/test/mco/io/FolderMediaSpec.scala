package mco.io

import mco.{Media, UnitSpec}
import mco.io.files.{IO, Path}
import IOInterpreters._
import IOInterpreters.FSDsl._

class FolderMediaSpec extends UnitSpec {
  "FolderMedia companion" should "create media for folders" in {
    val (state, _) = pureTestMedia
    val media = StubIORunner(state).value(FolderMedia("seed"))
    media should not be empty
  }

  it should "not create media for regular files" in {
    val (state, _) = pureTestMedia

    val media = Seq("regular.file", "archive.7z")
      .map(FolderMedia)
      .map(StubIORunner(state).value _)

    all (media) shouldBe empty
  }

  it should "not create media for nonexistend dir" in {
    val (state, _) = pureTestMedia
    StubIORunner(state).value(FolderMedia("/nope/does/not/exist")) shouldBe empty
  }

  private def pureTestMedia: (Dir, Media[IO]) = {
    val state = fs(
      "seed" -> dir("file1" -> obj(), "file2" -> obj(), "file3" -> obj()),
      "regular.file" -> obj(),
      "archive.7z" -> arc("test" -> obj())
    )
    val media = StubIORunner(state)
      .value(FolderMedia("seed"))
      .getOrElse(fail("Could not create media"))

    (state, media)
  }

  "FolderMedia#readContent" should "list contained files with their hashes" in {
    val (state, media) = pureTestMedia
    val contents = StubIORunner(state).value(media.readContent)
    contents should contain theSameElementsAs Set("file1", "file2", "file3")
  }

  "FolderMedia#readData" should "load contained file contents" in {
    val (state, media) = pureTestMedia
    val op = media.copy(Map("file1" -> "fileZ"))
    val result = StubIORunner(state).state(op)
    deepGet(Path("fileZ"))(result) shouldBe Some(obj())
  }
}
