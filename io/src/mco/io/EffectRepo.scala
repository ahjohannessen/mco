package mco.io

import scala.language.higherKinds

import cats.data.Xor
import cats.{~>, Id}
import mco.{Fail, Package, Repository}

class EffectRepo[M[_], S](wrapped: Repository[M, S], nat: M ~> Id)
  extends Repository[Id, Unit]
{
  override def state: Unit = ()
  override def apply(key: String): Package = wrapped(key)
  override def packages: Traversable[Package] = wrapped.packages

  private val wrap = new EffectRepo(_: Repository[M, S], nat)

  override def change(oldKey: String, updates: Package): Self =
    wrap(nat(wrapped change (oldKey, updates)))

  override def add(f: String): Xor[Fail, Self] =
    nat(wrapped add f) map wrap

  override def remove(s: String): Xor[Fail, Self] =
    nat(wrapped remove s) map wrap
}
