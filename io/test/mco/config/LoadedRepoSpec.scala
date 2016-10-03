package mco.config

import mco.UnitSpec
import mco.io.IOInterpreters._
import FSDsl._
import cats.arrow.FunctionK
import mco.io.files.IO
import org.scalatest.tagobjects.Slow

class LoadedRepoSpec extends UnitSpec {
  "LoadedRepo#apply" should "convert config and transformation to repository" taggedAs Slow in {
    val repo = LoadedRepo(config, nat)
    StubIORunner(state).value(repo) should be a 'success
    val repo2 = LoadedRepo(config.copy(persistency = Persistency.JSON), nat)
    StubIORunner(state).value(repo2) should be a 'success
  }

  it should "fail if config does not point to valid classifiers or media" taggedAs Slow in {
    val repo = LoadedRepo(config.copy(classifier = "mco.io.Classifiers.faux"), nat)
    StubIORunner(state).value(repo) should be a 'failure
    val media = Vector("mco.io.FolderMedia", "mco.io.FauxMedia")
    val repo2 = LoadedRepo(config.copy(media = media), nat)
    StubIORunner(state).value(repo2) should be a 'failure
  }

  it should "fail if config source value does not point to a folder" taggedAs Slow in {
    val repo = LoadedRepo(config.copy(source="non_existent"), nat)
    StubIORunner(state).value(repo) should be a 'failure
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

  private def nat = FunctionK.id[IO]
}
