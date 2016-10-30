package mco
import ContentKind._

class ContentKindSpec extends UnitSpec {
  "ContentKind#asString and fromString" should "be opposite of each other" in {
    for (kind <- Seq(Mod, Doc, Garbage)) {
      fromString(asString(kind)) shouldBe Some(kind)
    }

    for (str <- Seq("Mod", "Doc", "Garbage")) {
      fromString(str).map(asString) shouldBe Some(str)
    }
  }

  "ContentKind#fromString" should "return None for strings that cannot be mapped" in {
    for (malformed <- Seq("foo", "Eggs", "doc")) {
      fromString(malformed) shouldBe empty
    }
  }
}
