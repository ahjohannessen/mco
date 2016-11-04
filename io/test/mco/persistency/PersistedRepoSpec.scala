package mco.persistency

import java.net.URL

import cats.Id
import mco.io.Fail
import mco.{Package, Repository, Thumbnail, UnitSpec}

class PersistedRepoSpec extends UnitSpec {
  "PersistedRepo" should "lift state updates to methods return values" in {
    val (storeOp, _) = new PersistedRepo(stub()).change("", x => x)
    val nextState = stub().change("", x => x).state
    storeOp should equal (Update(stub().state, nextState))

    val (storeOp2, _) = new PersistedRepo(stub()).add("key")
    val nextState2 = stub().add("key").state
    storeOp2 should equal (Update(stub().state, nextState2))

    val (thumbnailOp, _) = new PersistedRepo(stub()).thumbnail("foo").url
    thumbnailOp shouldBe NoOp
  }

  it should "propagate failures without any further operation" in {
    a [Fail.NameConflict] shouldBe thrownBy{
      new PersistedRepo(stub()).remove("failure is expected")
    }
  }

  it should "only return Unit value as its state" in {
    new PersistedRepo(stub()).state shouldBe { () }
  }

  it should "delegate #key, #apply and #packages to wrapped repo" in {
    val repo = stub(11)
    val persisted = new PersistedRepo(repo)
    for (s <- 1 to 10) persisted(s.toString) should equal (repo(s.toString))
    persisted.packages should equal (repo.packages)
    persisted.key should equal (repo.key)
  }

  def stub(i: Int = 0): Repository[Id, Int] = new Repository[Id, Int] {
    override def key: String = "test"
    override def state: Int = i
    override def apply(key: String): Package = Package(key, Set())
    override def thumbnail(key: String): Thumbnail[Id] = new Thumbnail[Id] {
      override def discardThumbnail: Unit = ()
      override def reassociate(to: String): Unit = ()
      override def url: Option[URL] = None
      override def setThumbnail(location: String): Unit = ()
    }
    override def packages: Traversable[Package] = Seq(Package("1", Set()), Package("2", Set()))
    override def change(oldKey: String, updates: Package): Self = stub(i * 11 + 17)
    override def add(f: String): Self = stub(i * 17 + 11)
    override def remove(s: String): Self = throw Fail.NameConflict("test exception")
  }
}
