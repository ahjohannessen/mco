package mco.core
package standard

import java.io.Serializable

import monad.IO
import general._

final class SimplePackage private (val data: PackageData, media: Media) extends Package {
  override type Metadata = Null

  override def install(rc: TargetContext): IO[(Package, Null)] = IO.flat { io =>
    val nextContents = data.contents.map {
      case cnt if cnt.kind.isInstallable =>
        val data = io(media readData cnt)
        io(rc output data)
        cnt.copy(isInstalled = true)
      case cnt => cnt
    }
    io(rc.flush())
    (copy(data.copy(contents = nextContents, isInstalled = true)), null)
  }

  override def remove(rc: TargetContext, meta: Null): IO[Package] = IO.flat { io =>
    val nextContents = data.contents.map {
      case cnt if cnt.isInstalled =>
        io(rc remove cnt.key)
        cnt.copy(isInstalled = false)
      case cnt => cnt
    }
    io(rc.flush())
    copy(data.copy(contents = nextContents, isInstalled = false))
  }

  private def copy(data: PackageData) = new SimplePackage(data, media)

  override def restoreMetadata(s: Serializable): Null = null
}


object SimplePackage extends Package.Companion {
  override protected def apply(data: PackageData, media: Media): Option[Package] =
    Some(new SimplePackage(data, media))

  override protected def reclassify(c: Content): ContentKind = ContentKind.Mod(true)
}