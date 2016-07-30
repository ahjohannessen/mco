package mco.core.general

import java.io.Serializable

import mco.core.monad.IO

trait Package {
  type Metadata <: Serializable

  val data: PackageData

  def restoreMetadata(s: Serializable): Metadata
  def install(rc: TargetContext): IO[(Package, Metadata)]
  def remove(rc: TargetContext, meta: Metadata): IO[Package]
}



object Package {
  trait Companion {
    final def apply(key: String, media: Media): IO[Option[Package]] = for {
      cs <- media.readContent
      rc = cs.map { c => c.copy(kind = reclassify(c)) }
      hash = media.hashMedia(cs)
    } yield apply(PackageData(key, rc, hash, isInstalled = false), media)

    protected def apply(data: PackageData, media: Media): Option[Package]

    protected def reclassify(c: Content): ContentKind
  }
}