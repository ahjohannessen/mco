package mco.io

import cats._

package object files {
  type IO[A] = MonadicIO.ops.IO[A]
  val IO: Monad[IO] with RecursiveTailRecM[IO] = cats.free.Free.catsFreeMonadForFree

  //noinspection TypeAnnotation
  val ops = MonadicIO.ops

  def unsafePerformIO[A](program: IO[A]): A = UnsafeIOInterpeter.run(program)
}
