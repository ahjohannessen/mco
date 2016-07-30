package mco
package core
package standard

import general.ContentKind.Mod
import general.stubs.TracingTargetContext
import general.{Media, StubGenerators}


class SimplePackageSpec extends UnitSpec with StubGenerators {

  "SimplePackage#install" should "provide all installable content into output" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)
      val installable = pkg.data.contents.filter(_.kind.isInstallable).to[Vector]

      val ctx = new TracingTargetContext
      (pkg install ctx).io()

      val provided = ctx.trace collect {
        case TracingTargetContext.Output(c) => c
      }

      provided shouldEqual installable

    }
  }

  it should "mark content as installed" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)
      val installable = pkg.data.contents.filter(_.kind.isInstallable).map(_.key)

      val (postInstall, _) = (pkg install new TracingTargetContext).io()
      val installed = postInstall.data.contents.filter(_.isInstalled).map(_.key)

      installed shouldEqual installable

    }
  }

  it should "close output context" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)
      val ctx = new TracingTargetContext
      (pkg install ctx).io()

      ctx.trace.last shouldBe TracingTargetContext.Close
    }
  }

  "SimplePackage#remove" should "uninstall all content which is installed" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)
      val removable = pkg.data.contents.filter(_.isInstalled).map(_.key)
      val ctx = new TracingTargetContext

      (pkg remove (ctx, null.asInstanceOf[pkg.Metadata])).io()
      val removed = ctx.trace
        .collect { case TracingTargetContext.Remove(k) => k }
        .to[Set]


      removed shouldEqual removable
    }
  }

  it should "unmark content as installed" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)

      val postInstall = (pkg remove (new TracingTargetContext, null.asInstanceOf[pkg.Metadata])).io()
      all (postInstall.data.contents) should have('isInstalled(false))
    }
  }

  it should "close output context" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)

      val ctx = new TracingTargetContext
      (pkg remove (ctx, null.asInstanceOf[pkg.Metadata])).io()

      ctx.trace.last shouldBe TracingTargetContext.Close
    }
  }

  "SimplePackage#restoreMetadata" should "return null" in {
    Option(packageFor(genMedia.sample.get).restoreMetadata(null)) shouldBe None
  }

  "SimplePackage companion" should "create package for any combination of data and media" in {
    forAll { (key: String, media: Media) =>
      SimplePackage(key, media).io() shouldBe defined
    }
  }

  it should "classify all objects as mods" in {
    forAll { (media: Media) =>
      val pkg = packageFor(media)
      all (pkg.data.contents) should have ('kind(Mod(true)))
    }
  }

  def packageFor(media: Media) = SimplePackage("test package", media).io().get
  def contentsOf(media: Media) = media.readContent.io()
}
