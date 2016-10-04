package mco.io

import cats.syntax.option._
import mco.UnitSpec
import mco.io.files._
import mco.io.IOInterpreters._
import mco.io.IOInterpreters.FSDsl._
import Stubs._
import Classifiers._

class FolderSourceSpec extends UnitSpec {
  private val stub = fs(
    "storage" -> dir(
      "file" -> obj(),
      "folder" -> dir(),
      "archive.zip" -> obj()
    ),
    "folder" -> dir(),
    "another file" -> obj(),
    "another folder" -> dir("named" -> obj()),
    "target" -> dir()
  )

  private val classifier = Classifiers.disableAll

  private val run = StubIORunner(stub)

  "FolderSource companion" should "create source given directory" in {
    noException shouldBe thrownBy {
      run.value(FolderSource("storage", classifier))
    }
  }

  it should "not create source given a file" in {
    a [Fail.UnexpectedType] shouldBe thrownBy {
      run.value(FolderSource("another file", classifier))
    }
  }

  "FolderSource#list" should "list objects created using provided media & classifier" in {
    val media1 = media("storage/archive.zip" -> randoms)
    val src1 = run.value(FolderSource("storage", disableAll, media1))
    val Some(expectedMedia1) = run.value(media1("storage/archive.zip"))
    run.value(src1.list).loneElement._1 shouldEqual run.value(disableAll(expectedMedia1))

    val media2 = media("storage/folder" -> randoms)
    val src2 = run.value(FolderSource("storage", enableAll, media2))
    val Some(expectedMedia2) = run.value(media2("storage/folder"))
    run.value(src2.list).loneElement._1 shouldEqual run.value(enableAll(expectedMedia2))
  }

  "FolderSource#add" should "pull file or folder from filesystem & source" in {
    val src = run.value(FolderSource("storage", classifier, media()))
    val state = run.state(src add "another folder")
    val added = deepGet(Path("storage/another folder"))(state)
    added shouldEqual dir("named" -> obj()).some

    val old = deepGet(Path("another folder"))(state)
    old should not be empty
  }

  it should "fail if the target element does not exist" in {
    val src = run.value(FolderSource("storage", classifier, media()))
    a [Fail.MissingResource] shouldBe thrownBy {
      run(src add "non-existent folder")
    }
  }

  it should "fail with NameConflict if the element with same key exists in the source dir" in {
    val src = run.value(FolderSource("storage", classifier, media("storage/folder" -> randoms)))
    a [Fail.NameConflict] shouldBe thrownBy {
      run(src add "folder")
    }
  }

  "FolderSource#remove" should "remove file or folder from filesystem & source" in {
    val src = run.value(FolderSource("storage", classifier, media("storage/folder" -> emptys, "storage/archive.zip" -> emptys)))
    val (state, value) = run(src remove "folder")
    deepGet(Path("storage/folder"))(state) shouldBe empty
    StubIORunner(state).value(value.list).loneElement._1.key shouldBe "archive.zip"

    val (state2, value2) = StubIORunner(state)(value remove "archive.zip")
    deepGet(Path("storage/archive.zip"))(state2) shouldBe empty
    StubIORunner(state2).value(value2.list) shouldBe empty
  }

  "FolderSource#rename" should "rename file or folder on disk" in {
    val src = run.value(FolderSource("storage", classifier, media("storage/folder" -> emptys, "storage/zondar" -> emptys)))
    val state = run.state(src rename ("folder", "zondar"))
    val Some(Dir(folderContents)) = deepGet(Path("storage"))(state)
    folderContents shouldNot contain key "folder"
    folderContents should contain key "zondar"
    folderContents.get("zondar") shouldEqual deepGet(Path("storage/folder"))(stub)
  }

  it should "fail if the element with same name already exists" in {
    val src = run.value(FolderSource("storage", classifier, media("storage/folder" -> emptys, "storage/archive.zip" -> emptys)))

    a [Fail.NameConflict] shouldBe thrownBy {
      run(src rename ("folder", "archive.zip"))
    }
  }
}
