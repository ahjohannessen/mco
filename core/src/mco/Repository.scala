package mco

import scala.language.higherKinds

trait Repository[M[_], S] {
  final type Self = Repository[M, S]
  def state: S

  def apply(key: String): Package
  def packages: Traversable[Package]

  final def change(oldKey: String, update: Package => Package): M[Self] =
    change(oldKey, update(apply(oldKey)))

  def change(oldKey: String, updates: Package): M[Self]

  def add(f: String): M[Self]
  def remove(s: String): M[Self]

  final def widen: Self = this
}

object Repository {
  trait Companion[M[_], S] {
    def apply(source: Source[M], target: String, state: S): M[Repository[M, S]]
  }
}