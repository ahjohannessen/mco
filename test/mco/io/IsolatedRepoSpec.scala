package mco.io

import cats.data.{OptionT, Xor}
import mco.UnitSpec
import mco.general.{Content, ContentKind, Package}
import mco.io.IOInterpreters.FSDsl._
import mco.io.Stubs._
import mco.io.files.Path

class IsolatedRepoSpec extends UnitSpec {

  private val stub = fs(
    "source" -> dir(
      "package1" -> dir(
        "file.txt" -> obj()
      )
    ),
    "target" -> dir(
      "package1" -> dir(
        "content1" -> obj()
      )
    ),
    "renamed" -> obj()
  )

  val io = StubIORunner(stub)

  "IsolatedRepo companion" should "create a repository given a folder" in {
    val repoOpt = for {
      src <- OptionT(FolderSource("source", Classifiers.enableAll))
      repo <- OptionT.liftF(IsolatedRepo(src, Path("target"), ()))
    } yield repo

    io.value(repoOpt.value) should not be empty
  }

  private def repo = {
    val repoOptIO = for {
      src <- OptionT(FolderSource(
        "source", Classifiers.enableAll, media(
          "source/package1" -> Set("content1"),
          "source/renamed" -> Set("content1")
        ))
      )
      repo <- OptionT.liftF(IsolatedRepo(src, Path("target"), ()))
    } yield repo
    io value repoOptIO.value getOrElse fail("Expected repository to create successfully")
  }

  it should "preload created repository with existing data" in {
    repo("package1") shouldBe 'installed
  }

  "IsolatedRepo#state" should "just be Unit" in {
    repo.state shouldBe {}
  }

  "IsolatedRepo #apply & #packages" should "provide package information" in {
    repo("package1") shouldEqual Package(
      "package1",
      Set(Content("content1", ContentKind.Mod, isInstalled = true)),
      isInstalled = true
    )

    repo.packages should contain theSameElementsAs Seq(repo("package1"))
  }

  "IsolatedRepo#add" should "push object to file system and create a package from it" in {
    val addIO = repo add "renamed"
    val (state, Xor.Right(nextRepo)) = io(addIO)
    nextRepo.packages.map(_.key) should contain theSameElementsAs Seq("package1", "renamed")
    deepGet(Path("source/renamed"))(state) should not be empty
  }

  "IsolatedRepo#remove" should "remove object from fs and its package" in {
    val removeIO = repo remove "package1"
    val (state, Xor.Right(nextRepo)) = io(removeIO)
    nextRepo.packages shouldBe empty
    deepGet(Path("source/package1"))(state) shouldBe empty
    deepGet(Path("target/package1"))(state) shouldBe empty
  }

  "IsolatedRepo#change" should "rename package if name is changed" in {
    val changeIO = repo.change("package1", _.copy(key = "renamed"))
    val (state, nextRepo) = io(changeIO)

    nextRepo.packages.find(_.key == "package1") shouldBe empty
    nextRepo.packages.loneElement.key shouldEqual "renamed"
    nextRepo.packages.loneElement shouldBe 'installed

    deepGet(Path("target/package1"))(state) shouldBe empty
    deepGet(Path("target/renamed/content1"))(state) shouldEqual Some(obj())
  }

  it should "install/uninstall package if its status changes" in {
    val changeIO = repo.change("package1", _.copy(isInstalled = false))
    val (state, nextRepo) = io(changeIO)

    nextRepo("package1") should not be 'installed
    deepGet(Path("target/package1"))(state) shouldBe empty

    val changeIO2 = nextRepo.change("package1", _.copy(isInstalled = true))
    val (state2, nextRepo2) = StubIORunner(state).apply(changeIO2)
    deepGet(Path("target/package1/content1"))(state2) should not be empty
    nextRepo2("package1") shouldBe 'installed
  }

  it should "update status of every package content" in {
    val disableAll = repo("package1").contents map { _.copy(kind = ContentKind.Garbage) }
    val changeIO = repo.change("package1", _.copy(contents = disableAll))
    val (state, nextRepo) = io(changeIO)

    nextRepo("package1").contents.loneElement should not be 'installed
    deepGet(Path("target/package1/content1"))(state) shouldBe empty
  }
}
