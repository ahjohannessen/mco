package mco.io

import better.files.File
import mco.io.files.{Path, UnsafeIOInterpreter}
import org.scalatest.{Matchers, Outcome, fixture}
import UnsafeIOInterpreter._
import net.sf.sevenzipjbinding.SevenZip

class UnsafeIOSpec extends fixture.FlatSpec with Matchers {
  "UnsafeIOInterpreter#childrenOf" should "list children of directory" in { dir =>
    val expected = Seq("test_folder", "test_archive.7z", "test_archive.rar", "test_archive.zip")
    val actual = UnsafeIOInterpreter.childrenOf(dir) map (_ relativeToS dir)
    actual should contain theSameElementsAs expected
  }

  "UnsafeIOInterpreter#descendantsOf" should "list descendants of directory" in { dir =>
    descendantsOf(dir).map(_ relativeToS dir) should contain theSameElementsAs Seq(
      "test_folder",
      "test_folder/file1",
      "test_folder/file2",
      "test_folder/file3",
      "test_archive.7z",
      "test_archive.rar",
      "test_archive.zip"
    )
  }

  "UnsafeIOInterpreter#removeFile" should "remove file or directory completely" in { dir =>
    for { path <- Seq(dir / "test_folder", dir / "test_archive.rar") } {
      removeFile(path)
      isDirectory(path) shouldBe false
      isRegularFile(path) shouldBe false
    }
  }

  it should "do nothing if nothing exists at given path" in { dir =>
    noException should be thrownBy {
      removeFile(dir / "does" / "not" / "exist")
    }
  }

  "UnsafeIOInterpreter#isRegularFile" should "correctly check if path represents a file" in { dir =>
    isRegularFile(dir / "test_folder") shouldBe false
    isRegularFile(dir / "test_folder" / "file1") shouldBe true
    isRegularFile(dir / "test_archive.7z") shouldBe true
  }

  "UnsafeIOInterpreter#isDirectory" should "correctly check if path represents a file" in { dir =>
    isDirectory(dir / "test_folder") shouldBe true
    isDirectory(dir / "test_folder" / "file1") shouldBe false
    isDirectory(dir / "test_archive.7z") shouldBe false
  }

  "UnsafeIOInterpreter#archiveEntries" should "list archive entries" in { dir =>
    archiveEntries(dir / "test_archive.zip") should contain theSameElementsAs Seq("file1", "file2", "file3")
  }

  "UnsafeIOInterpreter#extract" should "extract archive contents according to mapping" in { dir =>
    val target = dir / "file2.txt"
    extract(dir / "test_archive.rar", Map("file2" -> target))
    readBytes(target) should contain theSameElementsAs "World".getBytes
  }

  "UnsafeIOInterpreter#readBytes" should "read file content as byte array" in { dir =>
    val toRead = dir / "test_folder" / "file1"
    readBytes(toRead) should contain theSameElementsAs "Hello".getBytes
  }

  "UnsafeIOInterpreter#setContent" should "overwrite or create file with given contents" in { dir =>
    val toOverwrite = dir / "test_archive.7z"
    val toCreate = dir / "new.file"

    for (f <- Seq(toOverwrite, toCreate)) {
      val bytes = "Hello".getBytes
      setContent(f, bytes)
      isRegularFile(f) shouldBe true
      readBytes(f) should contain theSameElementsAs bytes
    }
  }

  "UnsafeIOInterpreter#createDirectory" should "create directory with all parents" in { dir =>
    val subdir = dir / "sub" / "path" / "deep"
    createDirectory(subdir)

    isDirectory(subdir) shouldBe true
  }

  "UnsafeIOInterpreter#copyTree" should "copy files and folders" in { dir =>
    val fld = dir / "test_folder"
    copyTree(dir / "test_archive.7z", fld / "arc.7z")

    readBytes(dir / "test_archive.7z") should contain theSameElementsAs readBytes(fld / "arc.7z")

    val moved = dir / "test_folder_moved"
    copyTree(fld, moved)

    isDirectory(fld) shouldBe true
    isDirectory(moved) shouldBe true

    val childrenBeforeCopy = descendantsOf(fld).map(_ relativeToS fld)
    val childrenAfterCopy = descendantsOf(moved).map(_ relativeToS moved)
    childrenBeforeCopy should contain theSameElementsAs childrenAfterCopy
  }

  "UnsafeIOInterpreter#moveTree" should "move files and folders" in { dir =>
    val testFolder = dir / "test_folder"
    moveTree(dir / "test_archive.7z", testFolder / "arc.7z")

    isRegularFile(dir / "test_archive.7z") shouldBe false
    isRegularFile(testFolder / "arc.7z") shouldBe true
    val oldChildren = descendantsOf(testFolder)
      .map(_ relativeToS testFolder)
      .force

    val moved = dir / "test_folder_moved"
    moveTree(testFolder, moved)

    isDirectory(testFolder) shouldBe false
    isDirectory(moved) shouldBe true

    descendantsOf(moved).map(_ relativeToS moved) should contain theSameElementsAs oldChildren
  }

  override type FixtureParam = Path
  override protected def withFixture(test: OneArgTest): Outcome = {
    val dir = File.newTemporaryDirectory("mco-io-tests")
    val nativeDir = File.newTemporaryDirectory("mco-io-tests-nativelibs")
    nativeDir.toJava.deleteOnExit()
    val statics: File = File(getClass.getResource("/mco.io.unsafe").toURI)
    statics.copyTo(dir, overwrite = true)
    try {
      SevenZip.initSevenZipFromPlatformJAR(nativeDir.toJava)
      withFixture(test.toNoArgTest(Path(dir)))
    } finally {
      dir.delete(swallowIOExceptions = true)
      ()
    }
  }
}
