package mco.persistency

import scala.language.higherKinds

import cats.Functor
import cats.data.Xor
import mco.{Fail, Package, Repository}
import cats.syntax.functor._
import cats.syntax.bifunctor._
import cats.instances.tuple._


final class PersistedRepo[M[_]: Functor, S](current: Repository[M, S])
  extends Repository[λ[α => M[(StoreOp[S], α)]], Unit] {

  override def state: Unit = ()

  override def apply(key: String): Package = current(key)
  override def packages: Traversable[Package] = current.packages

  private def recordUpdate(next: Repository[M, S]) = {
    (Update(current.state, next.state): StoreOp[S], new PersistedRepo(next).widen)
  }

  private def recordXorUpdate(xor: Fail Xor Repository[M, S]): (StoreOp[S], Fail Xor Self) =
    xor.fold(
      fail => (NoOp(current.state), Xor.left(fail)),
      repo => recordUpdate(repo) bimap (identity, Xor.right)
    )

  override def change(oldKey: String, updates: Package): M[(StoreOp[S], Self)] =
    current change (oldKey, updates) map recordUpdate

  override def add(f: String): M[(StoreOp[S], Fail Xor Self)] =
    current add f map recordXorUpdate

  override def remove(s: String): M[(StoreOp[S], Fail Xor Self)] = {
    current remove s map recordXorUpdate
  }
}
