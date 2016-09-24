package mco.persistency

import cats.Id
import cats.data.Xor
import mco.Fail.Uncaught
import mco.{Fail, Package, Repository, UnitSpec}

class PersistedRepoSpec extends UnitSpec {
  "PersistedRepo" should "lift state updates to methods return values" in {
    val (storeOp, _) = new PersistedRepo(stub()).change("", x => x)
    val nextState = stub().change("", x => x).state
    storeOp should equal (Update(stub().state, nextState))

    val (storeOp2, _) = new PersistedRepo(stub()).add("key")
    val nextState2 = stub().add("key").getOrElse(fail("expected Right")).state
    storeOp2 should equal (Update(stub().state, nextState2))
  }

  it should "respond to failures with NoOp" in {
    val (storeOp, _) = new PersistedRepo(stub()).remove("failure is expected")
    storeOp shouldBe a [NoOp[_]]
  }

  it should "only return Unit value as its state" in {
    new PersistedRepo(stub()).state shouldBe { () }
  }

  it should "delegate #apply and #packages to wrapped repo" in {
    val repo = stub(11)
    val persisted = new PersistedRepo(repo)
    for (s <- 1 to 10) persisted(s.toString) should equal (repo(s.toString))
    persisted.packages should equal (repo.packages)
  }

  def stub(i: Int = 0): Repository[Id, Int] = new Repository[Id, Int] {
    override def state: Int = i
    override def apply(key: String): Package = Package(key, Set())
    override def packages: Traversable[Package] = Seq(Package("1", Set()), Package("2", Set()))
    override def change(oldKey: String, updates: Package): Self = stub(i * 11 + 17)
    override def add(f: String): Id[Xor[Fail, Self]] = Xor.right(stub(i * 17 + 11))
    override def remove(s: String): Id[Xor[Fail, Self]] = Xor.left(Uncaught(new Exception))
  }
}
