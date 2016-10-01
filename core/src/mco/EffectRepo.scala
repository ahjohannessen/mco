package mco

import scala.language.higherKinds

import cats.data.Xor
import cats.~>
import monix.eval.Task

class EffectRepo[M[_], S](wrapped: Repository[M, S], nat: M ~> Task)
  extends Repository[Task, Unit]
{
  override def state: Unit = ()
  override def apply(key: String): Package = wrapped(key)
  override def packages: Traversable[Package] = wrapped.packages

  private val wrap = new EffectRepo(_: Repository[M, S], nat)

  override def change(oldKey: String, updates: Package): Task[Self] =
    nat(wrapped change (oldKey, updates)) map wrap

  override def add(f: String): Task[Xor[Fail, Self]] =
    nat(wrapped add f) map (_ map wrap)

  override def remove(s: String): Task[Xor[Fail, Self]] =
    nat(wrapped remove s) map (_ map wrap)
}
