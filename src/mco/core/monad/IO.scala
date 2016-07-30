package mco.core.monad

import scala.annotation.tailrec
import scala.language.higherKinds


sealed trait IO[+A] {
  def map[B](f: A => B) = flatMap(a => IO(f(a)))
  def flatMap[B](f: A => IO[B]): IO[B] = IO.FlatMap(this, f)
  override def toString = "IO[_]"
}

object IO {
  def apply[A](body: => A): IO[A] = Result(() => body)

  def flat[A](body: (Executor) => A): IO[A] = IO { body(Executor) }

  def once[A](body: => A): IO[A] = {
    lazy val stored = body
    IO(stored)
  }

  def unsafePerform[A](io: IO[A]): A = Executor(io)

  sealed trait Executor {
    @tailrec final def apply[A](io: IO[A]): A = io match {
      case Result(get) => get()
      case FlatMap(a, f: (Any => IO[A])) => a match {
        case Result(fn) => apply(f(fn()))
        case FlatMap(b, g: (Any => IO[_])) => apply(b flatMap { (x: Any) => g(x) flatMap f})
      }
    }
  }

  private object Executor extends Executor

  private case class Result[A](get: () => A) extends IO[A]
  private case class FlatMap[A, B](base: IO[B], tr: B => IO[A]) extends IO[A]
}
