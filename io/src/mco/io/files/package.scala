package mco.io

import cats._
import cats.data.{Xor, XorT}
import mco.Fail

package object files {
  type IO[A] = MonadicIO.ops.IO[A]
  val IO: Monad[IO] with RecursiveTailRecM[IO] = cats.free.Free.catsFreeMonadForFree

  //noinspection TypeAnnotation
  val ops = MonadicIO.ops

  type UnsafeIO[B] = XorT[IO, Fail, B]
  object UnsafeIO {
    def apply[B](b: IO[Fail Xor B]): UnsafeIO[B] = XorT(b)
    def liftT[B](b: IO[B]): UnsafeIO[B] = XorT.liftT(b)
  }

  def unsafePerformIO[A](program: IO[A]): A = UnsafeIOInterpeter.run(program)
}
