package mco.persistency

import scala.language.higherKinds

import cats.Functor
import cats.data.Xor
import mco.{Fail, Package, Repository}
import cats.syntax.functor._
import cats.syntax.bifunctor._
import cats.instances.tuple._


final class PersistedRepo[M[_]: Functor, S](wrapped: Repository[M, S])
  extends Repository[λ[α => M[(StoreOp[S], α)]], Unit] {

  override def state: Unit = ()

  override def apply(key: String): Package = wrapped(key)
  override def packages: Traversable[Package] = wrapped.packages

  private def recordUpdate(next: Repository[M, S]) = {
    (Update(wrapped.state, wrapped.state): StoreOp[S], new PersistedRepo(next).widen)
  }

  private def recordXorUpdate(xor: Fail Xor Repository[M, S]): (StoreOp[S], Xor[Fail, Self]) =
    xor.fold(
      fail => (NoOp, Xor.left(fail)),
      repo => recordUpdate(repo) bimap (identity, Xor.right)
    )

  override def change(oldKey: String, updates: Package): M[(StoreOp[S], Self)] =
    wrapped change (oldKey, updates) map recordUpdate

  override def add(f: String): M[(StoreOp[S], Fail Xor Self)] =
    wrapped add f map recordXorUpdate

  override def remove(s: String): M[(StoreOp[S], Fail Xor Self)] = {
    wrapped remove s map recordXorUpdate
  }
}