package mco
package core
package mco.general

import org.scalacheck.{Arbitrary, Gen}
import Arbitrary.arbitrary
import ContentKind._
import stubs.NoopMedia

trait StubGenerators {
  lazy val genContentKind: Gen[ContentKind] = Gen.oneOf(Seq(Mod(true), Mod(false), Asset, Doc, Garbage(None), Garbage(Some(Asset))))

  implicit lazy val arbContentKind = Arbitrary(genContentKind)

  lazy val genContent: Gen[Content] = for {
    key <- arbitrary[String]
    h1 <- arbitrary[Long]
    h2 <- arbitrary[Long]
    kind <- arbitrary[ContentKind]
    isInstalled <- arbitrary[Boolean]
  } yield Content(key, Hash(h1, h2), kind, isInstalled)

  implicit lazy val arbContent = Arbitrary(genContent)

  lazy val genMedia: Gen[Media] = for {
    contents <- arbitrary[Seq[Content]]
  } yield new NoopMedia(contents: _*)

  implicit lazy val arbMedia = Arbitrary(genMedia)
}
