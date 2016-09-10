package mco.io

import cats.data.Xor._
import mco.UnitSpec
import Files._
import mco.io.IOInterpreters._
import mco.io.IOInterpreters.FSDsl._
import Stubs._

class FolderSourceSpec extends UnitSpec {
  private val stub = fs(
    "storage" -> dir(
      "file" -> obj(),
      "folder" -> obj(),
      "archive.zip" -> obj()
    ),
    "another file" -> obj(),
    "another folder" -> dir("named" -> obj()),
    "target" -> dir()
  )

  private val classifier = Classifiers.noncustomizable

  private val run = StubRun(stub)

  "FolderSource companion" should "create source given directory" in {
    val io = FolderSource("storage", classifier)
    run.value(io) should not be empty
  }

  it should "not create source given a file" in {
    val io = FolderSource("another file", classifier)
    run.value(io) shouldBe empty
  }

  "FolderSource#list" should "list objects created using provided media & classifier" in {
    import Classifiers._
    val media1 = media("storage/archive.zip" -> randoms)
    val Some(src1) = run.value(FolderSource("storage", noncustomizable, media1))
    val Some(expectedMedia1) = run.value(media1("storage/archive.zip"))
    run.value(src1.list).loneElement._1 shouldEqual run.value(noncustomizable(expectedMedia1))

    val media2 = media("storage/folder" -> randoms)
    val Some(src2) = run.value(FolderSource("storage", customizable, media2))
    val Some(expectedMedia2) = run.value(media2("storage/folder"))
    run.value(src2.list).loneElement._1 shouldEqual run.value(customizable(expectedMedia2))
  }

  "FolderSource#add" should "pull file or folder from filesystem & source" in {
    val Some(src) = run.value(FolderSource("storage", classifier, media()))
    val state = run.state(src add "another folder")
    val added = deepGet(Path("storage/another folder"))(state)
    added shouldEqual dir("named" -> obj()).some

    val old = deepGet(Path("another folder"))(state)
    old should not be empty
  }

  "FolderSource#remove" should "remove file or folder from filesystem & source" in {
    val Some(src) = run.value(FolderSource("storage", classifier, media("storage/folder" -> emptys, "storage/archive.zip" -> emptys)))
    val (state, Right(value)) = run(src remove "folder")
    deepGet(Path("storage/folder"))(state) shouldBe empty
    StubRun(state).value(value.list).loneElement._1.key shouldBe "archive.zip"

    val (state2, Right(value2)) = StubRun(state)(value remove "archive.zip")
    deepGet(Path("storage/archive.zip"))(state2) shouldBe empty
    StubRun(state2).value(value2.list) shouldBe empty
  }

  "FolderSource#rename" should "rename file or folder on disk" in {
    val Some(src) = run.value(FolderSource("storage", classifier, media("folder" -> emptys, "zondar" -> emptys)))
    val state = run.state(src rename ("folder", "zondar"))
    val Some(Dir(folderContents)) = deepGet(Path("storage"))(state)
    folderContents shouldNot contain key "folder"
    folderContents should contain key "zondar"
    folderContents.get("zondar") shouldEqual deepGet(Path("storage/folder"))(stub)
  }

  // TODO - specify failure conditions for FolderSource methods
}
