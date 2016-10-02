package mco.config

import mco.UnitSpec

class LoadedRepoSpec extends UnitSpec {
  "LoadedRepo#apply" should "convert config and natural transformation to repository" in {
    pending
  }

  it should "fail if the config provided does not point to valid classifiers or media" in {
    pending
  }

  it should "fail if config source value does not point to a folder" in {
    pending
  }

  def config = RepoConfig(
    key = "test_repo",
    kind = RepoKind.Isolated,
    source = "./source",
    target = "./target",
    classifier = "mco.io.Classifiers.enableAll",
    media = Vector("mco.io.FolderMedia", "mco.io.ArchiveMedia"),
    persistency = Persistency.None
  )
}
