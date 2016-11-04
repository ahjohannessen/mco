package mco

import java.net.URL

import cats.data.Const
import cats.{Id, ~>}

class ThumbnailSpec extends UnitSpec {
  "ThumbnailSpec#mapK" should "give Thumbnail with FunctionK applied to results" in {
    val thumbnail = new Thumbnail[Id] {
      override def url: Option[URL] = None
      override def setThumbnail(location: String): Unit = ()
      override def discardThumbnail: Unit = ()
      override def reassociate(to: String): Unit = ()
    }
    val nat = new (Id ~> Const[String, ?]) {
      override def apply[A](fa: A): Const[String, A] = fa match {
        case u: Unit => Const("Unit$")
        case None => Const("None$")
        case _ => fail("Unexpected adata")
      }
    }

    val mapped = thumbnail.mapK[Const[String, ?]](nat)

    mapped.url.getConst                       shouldBe "None$"
    mapped.setThumbnail("foo").getConst       shouldBe "Unit$"
    mapped.discardThumbnail.getConst          shouldBe "Unit$"
    mapped.reassociate("bar").getConst shouldBe "Unit$"
  }
}
