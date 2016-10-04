package mco.io.files

import cats.free.Free
import freasymonad.cats.free

// $COVERAGE-OFF$Macro-generated code
@free sealed trait MonadicIO {
  sealed trait OperationsADT[A]
  type FreeIO[A] = Free[OperationsADT, A]

  def childrenOf(path: Path)                     : FreeIO[Stream[Path]]
  def descendantsOf(path: Path)                  : FreeIO[Stream[Path]]
  def removeFile(path: Path)                     : FreeIO[Unit]
  def isRegularFile(path: Path)                  : FreeIO[Boolean]
  def isDirectory(path: Path)                    : FreeIO[Boolean]
  def archiveEntries(path: Path)                 : FreeIO[Set[String]]
  def extract(path: Path, ft: Map[String, Path]) : FreeIO[Unit]
  def readBytes(path: Path)                      : FreeIO[Array[Byte]]
  def setContent(path: Path, cnt: Array[Byte])   : FreeIO[Unit]
  def createDirectory(path: Path)                : FreeIO[Unit]
  def copyTree(source: Path, dest: Path)         : FreeIO[Unit]
  def moveTree(source: Path, dest: Path)         : FreeIO[Unit]
}
// $COVERAGE-ON
