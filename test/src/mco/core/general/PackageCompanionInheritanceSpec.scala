package mco
package core
package general

import mco.core.general.Package.Companion

class PackageCompanionInheritanceSpec extends UnitSpec with StubGenerators {
  "Application of Package companion object" should "return None if protected override does" in {
    val impl = new Package.Companion {
      override protected def apply(data: PackageData, media: Media): Option[Package] = None
      override protected def reclassify(c: Content): ContentKind = c.kind
    }

    forAll { (key: String, m: Media) =>
      impl(key, m).io() shouldBe None
    }
  }


  it should "apply reclassification before protected apply is called" in {
    forAll { (m: Media, k: ContentKind) =>
        val impl = new Companion {
          override protected def reclassify(c: Content): ContentKind = k
          override protected def apply(data: PackageData, media: Media): Option[Package] = {
            all (data.contents) should have ('kind(k))
            None
          }
        }

        noException shouldBe thrownBy { impl("key", m).io() }
    }
  }
}
