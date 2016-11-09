package mco.persistency

import scala.language.higherKinds

import cats.syntax.functor._
import cats.{Functor, ~>}
import mco.{Package, Repository, Thumbnail}


final class PersistedRepo[M[_]: Functor, S](current: Repository[M, S])
  extends Repository[λ[α => M[(StoreOp[S], α)]], Unit] {

  override def key: String = current.key
  override def state: Unit = ()

  override def apply(key: String): Package = current(key)
  override def thumbnail(key: String): Thumbnail[PersistedRepo.Lift[M, S]#T] =
    current.thumbnail(key).mapK[PersistedRepo.Lift[M, S]#T](new (M ~> PersistedRepo.Lift[M, S]#T) {
      override def apply[A](fa: M[A]): M[(StoreOp[S], A)] = fa map withNoOp
    })

  override def packages: Traversable[Package] = current.packages

  private def recordUpdate(next: Repository[M, S]) = {
    (Update(current.state, next.state): StoreOp[S], new PersistedRepo(next).widen)
  }

  private def withNoOp[A](a: A) = (NoOp, a)

  override def change(oldKey: String, updates: Package): M[(StoreOp[S], Self)] =
    current change (oldKey, updates) map recordUpdate

  override def add(f: String): M[(StoreOp[S], (Package, Self))] =
    current add f map { case (pkg, nextRepo) =>
      val (op, nextPersisted) = recordUpdate(nextRepo)
      (op, (pkg, nextPersisted))
    }

  override def remove(s: String): M[(StoreOp[S], Self)] = {
    current remove s map recordUpdate
  }

  override def canAdd(f: String): M[(StoreOp[S], Boolean)] = (current canAdd f) map withNoOp
}

object PersistedRepo {
  type Lift[M[_], S] = { type T[A] = M[(StoreOp[S], A)] }
}
