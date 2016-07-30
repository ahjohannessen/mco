package mco
package core
package general

import stubs.NoopMedia

class MediaSpec extends UnitSpec with StubGenerators {
  "Media#hashMedia" should "be associative" in {
    forAll { (m1: Media, m2: Media) =>
      val Seq(cs1, cs2) = Seq(m1, m2) map contentsOf
      whenever (cs1.size + cs2.size == (cs1 ++ cs2).size) {
        val contentWith1stHash = Content("<dummy>", contentHashOf(m1))
        val contentWith2ndHash = Content("<dummy>", contentHashOf(m2))

        contentHashOf(new NoopMedia(contentWith1stHash)) shouldEqual contentHashOf(m1)
        contentHashOf(new NoopMedia(contentWith2ndHash)) shouldEqual contentHashOf(m2)

        val hashesMedia = new NoopMedia(contentWith1stHash, contentWith2ndHash)
        val contentsMedia = new NoopMedia((cs1 ++ cs2).toSeq: _*)

        contentHashOf(hashesMedia) shouldEqual contentHashOf(contentsMedia)
      }
    }
  }

  it should "return (0, 0) as hash for empty content" in {
    val empty = new NoopMedia()
    contentHashOf(empty) shouldBe Hash(0, 0)
  }

  def contentsOf(media: Media) = media.readContent.io()
  def contentHashOf(media: Media) = media.hashMedia(contentsOf(media))
}
