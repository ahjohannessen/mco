package mco.io.files

import cats._
import cats.free.Free
import freasymonad.free

// $COVERAGE-OFF$Macro-generated code
@free sealed trait MonadicIO {
  type IO[A] = Free[OperationsADT, A]
  def IO: Monad[IO] with RecursiveTailRecM[IO] = implicitly
  sealed trait OperationsADT[A]

  def childrenOf(path: Path)                     : IO[Stream[Path]]
  def descendantsOf(path: Path)                  : IO[Stream[Path]]
  def removeFile(path: Path)                     : IO[Unit]
  def isRegularFile(path: Path)                  : IO[Boolean]
  def isDirectory(path: Path)                    : IO[Boolean]
  def archiveEntries(path: Path)                 : IO[Set[String]]
  def extract(path: Path, ft: Map[String, Path]) : IO[Unit]
  def readBytes(path: Path)                      : IO[Array[Byte]]
  def setContent(path: Path, cnt: Array[Byte])   : IO[Unit]
  def createDirectory(path: Path)                : IO[Unit]
  def copyTree(source: Path, dest: Path)         : IO[Unit]
  def moveTree(source: Path, dest: Path)         : IO[Unit]
}
// $COVERAGE-ON
