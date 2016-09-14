package mco.general

import scala.language.higherKinds

import cats.data.Xor

trait Repository[M[_], T] {
  type State

  def state: State

  def apply(key: String): Package
  def packages: Traversable[Package]

  final def change(oldKey: String, update: Package => Package): M[Repository.Aux[M, T, State]] =
    change(oldKey, update(apply(oldKey)))

  def change(oldKey: String, updates: Package): M[Repository.Aux[M, T, State]]

  def add(f: String): M[String Xor Repository.Aux[M, T, State]]
  def remove(s: String): M[String Xor Repository.Aux[M, T, State]]
}

object Repository {
  type Aux[M[_], T, S] = Repository[M, T] { type State = S }
  trait Companion[M[_], T, S] {
    def apply(source: Source[M], target: T, state: S): M[Repository.Aux[M, T, S]]
  }
}