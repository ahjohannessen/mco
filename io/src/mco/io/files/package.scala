package mco.io

import java.nio.file.attribute.BasicFileAttributes

import scala.language.{higherKinds, implicitConversions}
import scala.util.Try
import scala.util.control.NonFatal

import cats.{Monad, MonadError, RecursiveTailRecM}
import cats.data.{Xor, XorT}
import cats.syntax.all._

package object files {
  type IO[A] = XorT[MonadicIO.ops.FreeIO, Fail, A]
  implicit val IO: MonadError[IO, Fail] = XorT.catsDataMonadErrorForXorT[MonadicIO.ops.FreeIO, Fail]


  implicit class IOSyntax[A](val io: IO[A]) extends AnyVal {
    def unsafePerformWith[F[_]: Monad: RecursiveTailRecM](int: MonadicIO.Interp[F]): F[A] =
      int.run(io.value).map(_.fold(_.rethrow(), identity))

    def absorbTry[B](implicit ev: A <:< Try[B]): IO[B] =
      io.flatMap(x => Xor.fromTry(x).fold({
        case fail: Fail => fail.io[B]
        case NonFatal(ex) => Fail.Uncaught(ex).io[B]
      }, IO.pure))
  }

  implicit class FailSyntax(val fail: Fail) extends AnyVal {
    def io[A]: IO[A] = IO.raiseError[A](fail)
    def when(p: Boolean): IO[Unit] = if (p) fail.io[Unit] else ().pure[IO]
  }

//  implicit class OptionSyntax[A](val option: Option[A]) extends AnyVal {
//    def orRun(io: IO[A]): IO[A] = option map IO.pure getOrElse io
//    def orFail(failure: Fail): IO[A] = orRun(failure.io)
//    def orDie: IO[A] = orFail(Fail.InvariantViolation())
//  }

  implicit class IOOptionSyntax[A](val io: IO[Option[A]]) extends AnyVal {
    def orRun(alt: IO[A]): IO[A] = io flatMap (_ map IO.pure getOrElse alt)
    def orFail(failure: Fail): IO[A] = orRun(failure.io)
    def orDie: IO[A] = orFail(Fail.InvariantViolation())
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
  def stat(path: Path): IO[BasicFileAttributes] = ops.stat(path)

  def pathExists(path: Path) : IO[Boolean] =
    ops.isDirectory(path) |@| ops.isRegularFile(path) map (_ || _)

  def removeIfExists(path: Path): IO[Unit] = pathExists(path) flatMap
    { if (_) removeFile(path) else ().pure[IO] }

}
