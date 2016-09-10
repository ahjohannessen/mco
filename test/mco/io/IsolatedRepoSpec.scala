package mco.io

import mco.UnitSpec

class IsolatedRepoSpec extends UnitSpec {
  "IsolatedRepo companion" should "create a repository given a folder" in { ??? }

  it should "preload created repository with existing data" in { ??? }

  "IsolatedRepo#state" should "just be Unit" in { ??? }

  "IsolatedRepo #apply & #packages" should "provide package information using Source" in { ??? }

  "IsolatedRepo#add" should "push object to file system and create a package from it" in { ??? }

  "IsolatedRepo#remove" should "remove object from fs and its package" in { ??? }

  "IsolatedRepo#change" should "rename package if name is changed" in { ??? }

  it should "install/uninstall package if its status changes" in { ??? }

  it should "update status of every package content" in { ??? }
}
