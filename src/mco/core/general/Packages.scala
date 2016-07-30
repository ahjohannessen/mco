package mco.core.general

import scala.language.higherKinds

trait Packages[M[_]] {
  def all: Traversable[PackageData]
  def apply(key: String): PackageData

  def install  (key: String, target: TargetContext): M[Packages[M]]
  def uninstall(key: String, target: TargetContext): M[Packages[M]]
}
