package mco.config

import mco.UnitSpec
import mco.io.IOInterpreters._
import FSDsl._
import mco.io.Fail
import mco.io.files.IO
import org.scalatest.tagobjects.Slow

class LoadedRepoSpec extends UnitSpec {
  "LoadedRepo#apply" should "convert config to repository" taggedAs Slow in {
    noException shouldBe thrownBy {
      run(LoadedRepo(config))
      run(LoadedRepo(config.copy(persistency = Persistency.JSON)))
    }
  }

  it should "fail if config does not point to valid classifiers or media" taggedAs Slow in {
    a [Fail.MissingResource] shouldBe thrownBy {
      run(LoadedRepo(config.copy(classifier = "mco.io.Classifiers.phony")))
    }

    val media = Vector("mco.io.FolderMedia", "mco.io.PhonyMedia")
    a [Fail.MissingResource] shouldBe thrownBy {
      run(LoadedRepo(config.copy(media = media))) should be a 'failure
    }
  }

  it should "fail if config source value does not point to a folder" taggedAs Slow in {
    a [Fail.UnexpectedType] shouldBe thrownBy {
      run(LoadedRepo(config.copy(source = "non_existent")))
    }
  }

  private def config = RepoConfig(
    key = "test_repo",
    kind = RepoKind.Isolated,
    source = "source",
    target = "target",
    classifier = "mco.io.Classifiers.enableAll",
    media = Vector("mco.io.FolderMedia", "mco.io.ArchiveMedia"),
    persistency = Persistency.None
  )

  private def state = fs(
    "source" -> dir(),
    "target" -> dir()
  )

  private def run[A](r: IO[A]): A = StubIORunner(state).value(r)
}
