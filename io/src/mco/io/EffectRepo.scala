package mco.io

import scala.language.higherKinds

import cats.data.Xor
import cats.syntax.functor._
import cats.{Functor, ~>}
import mco.{Fail, Package, Repository}

class EffectRepo[F[_], G[_]: Functor, S](wrapped: Repository[F, S], nat: F ~> G)
  extends Repository[G, Unit]
{
  override def state: Unit = ()
  override def apply(key: String): Package = wrapped(key)
  override def packages: Traversable[Package] = wrapped.packages

  private val wrap = new EffectRepo(_: Repository[F, S], nat)

  override def change(oldKey: String, updates: Package): G[Self] =
    nat(wrapped change (oldKey, updates)) map wrap

  override def add(f: String): G[Fail Xor Self] =
    nat(wrapped add f) map (_ map wrap)

  override def remove(s: String): G[Fail Xor Self] =
    nat(wrapped remove s) map (_ map wrap)
}
