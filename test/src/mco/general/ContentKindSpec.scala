package mco.general

import ContentKind._
import mco.UnitSpec

class ContentKindSpec extends UnitSpec {
  "ContentKind.Garbage" should "allow overriding non-garbage" in {
    for (kind <- Seq(Mod(true), Mod(false), Asset, Doc)) noException shouldBe thrownBy {
      Garbage(Some(kind))
    }
  }

  it should "allow override to not be specified" in {
    noException shouldBe thrownBy (Garbage(None))
  }

  it should "not allow override to be set as another Garbage" in {
    an [IllegalArgumentException] shouldBe thrownBy {
      Garbage(Some(Garbage(None)))
    }
  }
}
