package mco.config

import com.typesafe.config.ConfigFactory
import mco.UnitSpec

class RepoConfigsSpec extends UnitSpec {
  "RepoConfigs#apply" should "read configuration from mco.io.repositories field" in {
    val result = RepoConfigs(ConfigFactory.parseString(
      """
        |mco.io.repositories = [{
        |  key = sims_2
        |  kind = Isolated
        |  source = source
        |  target = target
        |  classifier = mco.io.Classifiers.enableAll
        |  media = [ mco.io.FolderMedia ]
        |  persistency = None
        |}, {
        |  key = sims_3
        |  kind = Isolated
        |  source = source1
        |  target = target1
        |  classifier = mco.io.Classifiers.disableAll
        |  media = [ mco.io.ArchiveMedia ]
        |  persistency = JSON
        |}]
      """.stripMargin)) getOrElse fail("Expected success")

    result shouldBe Seq(
      RepoConfig(
        "sims_2",
        RepoKind.Isolated,
        "source",
        "target",
        "mco.io.Classifiers.enableAll",
        Vector("mco.io.FolderMedia"),
        Persistency.None
      ),
      RepoConfig(
        "sims_3",
        RepoKind.Isolated,
        "source1",
        "target1",
        "mco.io.Classifiers.disableAll",
        Vector("mco.io.ArchiveMedia"),
        Persistency.JSON
      )
    )
  }

  it should "fail if the field does not exist" in {
    val result = RepoConfigs(ConfigFactory.parseString(
      """
        |mco.io = {}
      """.stripMargin))

    result should be a 'failure
  }

  it should "fail if any of the configuration is specified incorrectly" in {
    val result = RepoConfigs(ConfigFactory.parseString(
      """
        |mco.io.repositories = [{
        |  key = test
        |  kind = Invalid
        |  source = source
        |  target = target
        |  classifier = mco.io.Classifiers.enableAll
        |  media = [ ]
        |  persistency = None
        |}]
      """.stripMargin))

    result should be a 'failure
  }
}
