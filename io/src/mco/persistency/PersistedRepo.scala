package mco.persistency

import java.net.URL

import scala.language.higherKinds

import cats.Functor
import mco.{Package, Repository}
import cats.syntax.functor._


final class PersistedRepo[M[_]: Functor, S](current: Repository[M, S])
  extends Repository[λ[α => M[(StoreOp[S], α)]], Unit] {

  override def key: String = current.key
  override def state: Unit = ()

  override def apply(key: String): Package = current(key)
  override def thumbnail(key: String): M[(StoreOp[S], Option[URL])] =
    current.thumbnail(key).fproduct(_ => NoOp).map(_.swap)

  override def packages: Traversable[Package] = current.packages

  private def recordUpdate(next: Repository[M, S]) = {
    (Update(current.state, next.state): StoreOp[S], new PersistedRepo(next).widen)
  }

  override def change(oldKey: String, updates: Package): M[(StoreOp[S], Self)] =
    current change (oldKey, updates) map recordUpdate

  override def add(f: String): M[(StoreOp[S], Self)] =
    current add f map recordUpdate

  override def remove(s: String): M[(StoreOp[S], Self)] = {
    current remove s map recordUpdate
  }
}

object PersistedRepo {
  type Lift[M[_], S] = { type T[A] = M[(StoreOp[S], A)] }
}
