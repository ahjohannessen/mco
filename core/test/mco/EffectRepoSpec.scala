package mco

import java.net.URL

import cats._
import cats.data.Const

class EffectRepoSpec extends UnitSpec {
  "EffectRepo#state" should "just be Unit value" in {
    val repo = new EffectRepo[(String, ?), Const[String, ?], Int](stub, nat)
    repo.state should be (())
  }

  "EffectRepo#key, apply and packages" should "delegate methods to wrapped repo" in {
    val repo = stub
    val effect = new EffectRepo[(String, ?), Const[String, ?], Int](repo, nat)
    for (s <- 1 to 10) effect(s.toString) should equal (repo(s.toString))
    effect.packages should equal (repo.packages)
    effect.key should equal (repo.key)
  }

  "EffectRepo#change, add, remove and thumbnail" should "have their effect transformed" in {
    val repo = new EffectRepo[(String, ?), Const[String, ?], Int](stub, nat)

    repo change ("foo", x => x) shouldBe Const("change")
    repo add "bar" shouldBe Const("add")
    repo remove "eggs" shouldBe Const("remove")
    repo.thumbnail("spam").url shouldBe Const("url")
  }

  private def stub: Repository[(String, ?), Int] = new Repository[(String, ?), Int] {
    override def key: String = "stub"

    override def state: Int = 42
    override def apply(key: String): Package = Package(key, Set())

    override def thumbnail(key: String): Thumbnail[(String, ?)] = new Thumbnail[(String, ?)] {
      override def discard: (String, Unit) = ("discardThumbnail", ())
      override def reassociate(to: String): (String, Unit) = ("reassociate", ())
      override def url: (String, Option[URL]) = ("url", None)
      override def setFrom(location: String): (String, Unit) = ("setThumbnail", ())
    }

    override def packages: Traversable[Package] = Seq(Package("1", Set()), Package("2", Set()))

    override def change(oldKey: String, updates: Package): (String, Self) = ("change", stub)

    override def add(f: String): (String, Self) = ("add", stub)

    override def remove(s: String): (String, Self) = ("remove", stub)
  }

  private def nat = new ((String, ?) ~> Const[String, ?]) {
    override def apply[A](fa: (String, A)): Const[String, A] = Const(fa._1)
  }
}
