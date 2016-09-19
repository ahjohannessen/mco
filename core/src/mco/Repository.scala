package mco

import scala.language.higherKinds

import cats.data.Xor

trait Repository[M[_]] {
  type State

  def state: State

  def apply(key: String): Package
  def packages: Traversable[Package]

  final def change(oldKey: String, update: Package => Package): M[Repository.Aux[M, State]] =
    change(oldKey, update(apply(oldKey)))

  def change(oldKey: String, updates: Package): M[Repository.Aux[M, State]]

  def add(f: String): M[String Xor Repository.Aux[M, State]]
  def remove(s: String): M[String Xor Repository.Aux[M, State]]
}

object Repository {
  type Aux[M[_], S] = Repository[M] { type State = S }
  trait Companion[M[_], S] {
    def apply(source: Source[M], target: String, state: S): M[Repository.Aux[M, S]]
  }
}