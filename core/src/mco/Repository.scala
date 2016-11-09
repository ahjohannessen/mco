package mco

import scala.language.higherKinds

import cats.Functor
import cats.syntax.functor._

trait Repository[M[_], S] {
  final type Self = Repository[M, S]

  def key: String
  def state: S

  def apply(key: String): Package
  def thumbnail(key: String): Thumbnail[M]
  def packages: Traversable[Package]

  final def change(oldKey: String, update: Package => Package): M[Self] =
    change(oldKey, update(apply(oldKey)))

  def change(oldKey: String, updates: Package): M[Self]

  def canAdd(f: String): M[Boolean]

  def add(f: String): M[(Package, Self)]

  final def add_(f: String)(implicit M: Functor[M]): M[Self] =
    this add f map { case (_, repo) => repo }

  def remove(s: String): M[Self]

  final def widen: Self = this
}

object Repository {
  trait Companion[M[_], S] {
    def apply(key: String, source: Source[M], target: String, state: S): M[Repository[M, S]]
  }
}
