package mco.core.standard

import mco.core.monad.IO
import mco.core.general._

final class SimplePackages private (packages: Map[String, Package]) extends Packages[IO] {
  override lazy val all: Traversable[PackageData] = packages.map(_._2.data)
  override def apply(key: String): PackageData = packages(key).data

  override def install(key: String, target: TargetContext): IO[Packages[IO]] =
    packages(key)
      .install(target)
      .map(t => new SimplePackages(packages + (key -> t._1)))


  override def uninstall(key: String, target: TargetContext): IO[Packages[IO]] = {
    val pkg = packages(key)
    pkg
      .remove(target, pkg.restoreMetadata(null))
      .map(p => new SimplePackages(packages + (key -> p)))
  }
}

object SimplePackages {
  def apply(src: Source[IO]) = IO.flat { io =>
    val medias = io(src.list)
    val packages = medias
      .par
      .flatMap({ SimplePackage(_, _) }.tupled andThen { io(_).toSeq })
      .map(p => p.data.key -> p)
      .seq
      .toMap
    new SimplePackages(packages)
  }
}