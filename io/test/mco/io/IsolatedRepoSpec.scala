package mco.io

import mco._
import mco.io.IOInterpreters.FSDsl._
import mco.io.Stubs._
import mco.io.files.Path

class IsolatedRepoSpec extends UnitSpec {

  private val stub = fs(
    "source" -> dir(
      "package1" -> dir(
        "file.txt" -> obj()
      ),
      "package2" -> dir(
        "readme.txt" -> obj(),
        "toInstall.txt" -> obj()
      )
    ),
    "target" -> dir(
      "package1" -> dir(
        "content1" -> obj()
      )
    ),
    "renamed" -> obj()
  )

  private val io = StubIORunner(stub)

  "IsolatedRepo companion" should "create a repository given a folder" in {
    val repoOpt = for {
      src <- FolderSource("source", Classifiers.enableAll)
      repo <- IsolatedRepo(src, "target", Set())
    } yield repo

    noException shouldBe thrownBy {
      io.value(repoOpt.value)
    }
  }

  private def repo = io.value(for {
    src <- FolderSource(
      "source", Classifiers.enableAll, media(
        "source/package1" -> Set("content1"),
        "source/package2" -> Set("readme.txt", "toInstall.txt"),
        "source/renamed" -> Set("content1")
      )
    )
    repo <- IsolatedRepo(src, "target", Set(Package(
      "package2",
      Set(Content("readme.txt", ContentKind.Doc), Content("toInstall.txt", ContentKind.Mod))
    )))
  } yield repo)

  it should "preload created repository with existing data" in {
    repo("package1") shouldBe 'installed
    repo("package2") should not be 'installed
  }

  it should "load additional data from state" in {
    repo("package2").contents should contain (Content("readme.txt", ContentKind.Doc))
  }

  "IsolatedRepo#state" should "simply contain package state" in {
    repo.state should contain theSameElementsAs repo.packages
  }

  "IsolatedRepo #apply & #packages" should "provide package information" in {
    repo("package1") shouldEqual Package(
      "package1",
      Set(Content("content1", ContentKind.Mod, isInstalled = true)),
      isInstalled = true
    )

    repo.packages should contain theSameElementsAs Seq(repo("package1"), repo("package2"))
  }

  "IsolatedRepo#add" should "push object to file system and create a package from it" in {
    val addIO = repo add "renamed"
    val (state, nextRepo) = io(addIO)
    nextRepo.packages.map(_.key) should contain theSameElementsAs Seq("package1", "package2", "renamed")
    deepGet(Path("source/renamed"))(state) should not be empty
  }

  "IsolatedRepo#remove" should "remove object from fs and its package" in {
    val removeIO = repo remove "package1"
    val (state, nextRepo) = io(removeIO)
    nextRepo.packages.loneElement.key shouldBe "package2"
    deepGet(Path("source/package1"))(state) shouldBe empty
    deepGet(Path("target/package1"))(state) shouldBe empty
  }

  "IsolatedRepo#change" should "rename package if name is changed" in {
    val changeIO = repo.change("package1", _.copy(key = "renamed"))
    val (state, nextRepo) = io(changeIO)

    nextRepo.packages.find(_.key == "package1") shouldBe empty
    nextRepo.packages.map(_.key) should contain ("renamed")
    nextRepo.packages.map(_.key) should not contain "package1"
    nextRepo("renamed") shouldBe 'installed

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

  it should "update status of every package content if package is installed" in {
    val disableAll = repo("package1").contents map { _.copy(kind = ContentKind.Garbage) }
    val changeIO = repo.change("package1", _.copy(contents = disableAll))
    val (state, nextRepo) = io(changeIO)

    nextRepo("package1").contents.loneElement should not be 'installed
    deepGet(Path("target/package1/content1"))(state) shouldBe empty
  }

  it should "update status of every package content if package is not installed" in {
    val changeIO = for {
      uninstall <- repo.change("package1", _.copy(isInstalled = false))
      disableAll = uninstall("package1").contents map { _.copy(kind = ContentKind.Garbage) }
      changed <- uninstall.change("package1", _.copy(contents = disableAll))
    } yield changed

    val (_, nextRepo) = io(changeIO)

    nextRepo("package1").contents.loneElement.kind shouldBe ContentKind.Garbage
  }
}
