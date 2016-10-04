package mco.io

import scala.language.{higherKinds, implicitConversions}
import scala.util.Try
import scala.util.control.NonFatal

import cats.{Monad, MonadError, RecursiveTailRecM}
import cats.data.{Xor, XorT}
import cats.syntax.functor._
import cats.syntax.cartesian._

package object files {
  type IO[A] = XorT[MonadicIO.ops.FreeIO, Fail, A]
  implicit val IO: MonadError[IO, Fail] = XorT.catsDataMonadErrorForXorT[MonadicIO.ops.FreeIO, Fail]


  implicit class IOSyntax[A](val io: IO[A]) extends AnyVal {
    def unsafePerformWith[F[_]: Monad: RecursiveTailRecM](int: MonadicIO.Interp[F]): F[A] =
      int.run(io.value).map(_.fold(_.rethrow(), identity))

    def absorbTry[B](implicit ev: A <:< Try[B]): IO[B] =
      io.flatMap(x => Xor.fromTry(x).fold({
        case fail: Fail => fail.as[B]
        case NonFatal(ex) => Fail.Uncaught(ex).as[B]
      }, IO.pure))
  }

  implicit class FailSyntax(val fail: Fail) extends AnyVal {
    def as[A]: IO[A] = IO.raiseError[A](fail)
    def when(p: Boolean): IO[Unit] = if (p) fail.as[Unit] else IO.pure(())
  }

  implicit def freeIOtoErrorIO[A](freeIO: MonadicIO.ops.FreeIO[A]): IO[A] =
    XorT.liftT[MonadicIO.ops.FreeIO, Fail, A](freeIO)

  private val ops = MonadicIO.ops

  // A chunk of boilerplate that triggers implicit conversion
  def childrenOf(path: Path) : IO[Stream[Path]] = ops.childrenOf(path)
  def descendantsOf(path: Path) : IO[Stream[Path]] = ops.descendantsOf(path)
  def removeFile(path: Path) : IO[Unit] = ops.removeFile(path)
  def isRegularFile(path: Path) : IO[Boolean] = ops.isRegularFile(path)
  def isDirectory(path: Path) : IO[Boolean] = ops.isDirectory(path)
  def archiveEntries(path: Path) : IO[Set[String]] = ops.archiveEntries(path)
  def extract(path: Path, ft: Map[String, Path]) : IO[Unit] = ops.extract(path, ft)
  def readBytes(path: Path) : IO[Array[Byte]] = ops.readBytes(path)
  def setContent(path: Path, cnt: Array[Byte]) : IO[Unit] = ops.setContent(path, cnt)
  def createDirectory(path: Path) : IO[Unit] = ops.createDirectory(path)
  def copyTree(source: Path, dest: Path) : IO[Unit] = ops.copyTree(source, dest)
  def moveTree(source: Path, dest: Path) : IO[Unit] = ops.moveTree(source, dest)

  def pathExists(path: Path) : IO[Boolean] =
    ops.isDirectory(path) |@| ops.isRegularFile(path) map (_ || _)

}
