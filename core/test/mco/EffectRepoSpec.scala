package mco

import cats._

class EffectRepoSpec extends UnitSpec {
  "EffectRepo#state" should "just be Unit value" in {
    val repo = new EffectRepo(stub, nat)
    repo.state should be (())
  }

  "EffectRepo#apply and packages" should "delegate methods to wrapped repo" in {
    val repo = stub
    val effect = new EffectRepo(repo, nat)
    for (s <- 1 to 10) effect(s.toString) should equal (repo(s.toString))
    effect.packages should equal (repo.packages)
  }

  "EffectRepo#change, add and remove" should "execute wrapped operations with side-effects" in {
    val repo = new EffectRepo(stub, nat)
    repo.change("foo", x => x)
    sideEffect should be ("change")

    repo.add("bar")
    sideEffect should be ("add")

    repo.remove("eggs")
    sideEffect should be ("remove")
  }

  private var sideEffect: String = ""

  private def stub: Repository[Eval, Int] = new Repository[Eval, Int] {
    override def state: Int = 42
    override def apply(key: String): Package = Package(key, Set())
    override def packages: Traversable[Package] = Seq(Package("1", Set()), Package("2", Set()))

    override def change(oldKey: String, updates: Package): Eval[Self] = Eval.always {
      sideEffect = "change"
      stub
    }

    override def add(f: String): Eval[Self] = Eval.always {
      sideEffect = "add"
      stub
    }

    override def remove(s: String): Eval[Self] = Eval.always {
      sideEffect = "remove"
      stub
    }
  }

  private def nat = new (Eval ~> Id) {
    override def apply[A](fa: Eval[A]): Id[A] = fa.value
  }
}
