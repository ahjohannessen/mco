package mco

import scala.language.higherKinds

import cats.data.Xor

trait Repository[M[_], S] {
  final type Self = Repository[M, S]
  def state: S

  def apply(key: String): Package
  def packages: Traversable[Package]

  final def change(oldKey: String, update: Package => Package): M[Self] =
    change(oldKey, update(apply(oldKey)))

  def change(oldKey: String, updates: Package): M[Self]

  def add(f: String): M[Fail Xor Self]
  def remove(s: String): M[Fail Xor Self]

  final def widen: Self = this
}

object Repository {
  trait Companion[M[_], S] {
    def apply(source: Source[M], target: String, state: S): M[Repository[M, S]]
  }
}