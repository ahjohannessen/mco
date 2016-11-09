package mco

import scala.language.higherKinds

import cats.syntax.functor._
import cats.instances.tuple._
import cats.{Functor, ~>}

class EffectRepo[F[_], G[_]: Functor, S](wrapped: Repository[F, S], nat: F ~> G)
  extends Repository[G, Unit]
{
  override def key: String = wrapped.key
  override def state: Unit = ()
  override def apply(key: String): Package = wrapped(key)
  override def thumbnail(key: String): Thumbnail[G] = wrapped.thumbnail(key).mapK(nat)
  override def packages: Traversable[Package] = wrapped.packages


  private val wrap = new EffectRepo(_: Repository[F, S], nat)

  override def change(oldKey: String, updates: Package): G[Self] =
    nat(wrapped change (oldKey, updates)) map wrap

  override def add(f: String): G[(Package, Self)] =
    nat(wrapped add f) map (_.map(wrap))

  override def remove(s: String): G[Self] =
    nat(wrapped remove s) map wrap

  override def canAdd(f: String): G[Boolean] = nat(wrapped canAdd f)
}
