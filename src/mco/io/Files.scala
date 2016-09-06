package mco.io

import better.files.File
import cats.{Monad, Id, ~>}
import cats.free.Free
import cats.free.Free.liftF
import com.olegpy.schive.Archive

object Files {
  case class Path(impure: File) {
    def relativeTo(other: Path) = Path(other.impure.relativize(impure))
    def asString = impure.pathAsString
    def fileName = impure.name
    def /(right: String) = Path(impure / right)
  }

  object Path extends (File => Path) {
    def apply(s: String): Path = apply(File(s))
  }

  object Ast {
    sealed trait FileOperationA[A]
    case class ChildrenOf(path: Path)                   extends FileOperationA[Stream[Path]]
    case class DescendantsOf(path: Path)                extends FileOperationA[Stream[Path]]
    case class RemoveFile(path: Path)                   extends FileOperationA[Unit]
    case class IsRegularFile(path: Path)                extends FileOperationA[Boolean]
    case class IsDirectory(path: Path)                  extends FileOperationA[Boolean]
    case class AsArchive(path: Path)                    extends FileOperationA[Archive]
    case class ReadBytes(path: Path)                    extends FileOperationA[Array[Byte]]
    case class SetContent(path: Path, cnt: Array[Byte]) extends FileOperationA[Unit]
    case class CreateDirectory(path: Path)              extends FileOperationA[Unit]
    case class Copy(source: Path, dest: Path)           extends FileOperationA[Unit]
    case class Move(source: Path, dest: Path)           extends FileOperationA[Unit]
  }

  import Ast._

  type IO[A] = Free[FileOperationA, A]
  val IO = Monad[IO]

  def childrenOf(path: Path)                   = liftF(ChildrenOf(path))
  def descendantsOf(path: Path)                = liftF(DescendantsOf(path))
  def removeFile(path: Path)                   = liftF(RemoveFile(path))
  def isRegularFile(path: Path)                = liftF(IsRegularFile(path))
  def isDirectory(path: Path)                  = liftF(IsDirectory(path))
  def asArchive(path: Path)                    = liftF(AsArchive(path))
  def readBytes(path: Path)                    = liftF(ReadBytes(path))
  def setContent(path: Path, cnt: Array[Byte]) = liftF(SetContent(path, cnt))
  def createDirectory(path: Path)              = liftF(CreateDirectory(path))
  def copy(source: Path, dest: Path)           = liftF(Copy(source, dest))
  def move(source: Path, dest: Path)           = liftF(Move(source, dest))

  private object UnsafeIO extends (FileOperationA ~> Id) {
    @inline final def discard[A](a: => A): Unit = { a; () }
    override def apply[A](fa: FileOperationA[A]): Id[A] = {
      val result = fa match {
        case ChildrenOf(path) => path.impure.children.map(Path).toStream
        case DescendantsOf(path) => path.impure.listRecursively.map(Path).toStream
        case RemoveFile(path) => discard { if (path.impure.exists) path.impure.delete() }
        case IsRegularFile(path) => path.impure.isRegularFile
        case IsDirectory(path) => path.impure.isDirectory
        case AsArchive(path) => Archive(path.impure.path)
        case ReadBytes(path) => path.impure.byteArray
        case SetContent(path, cnt) => discard { path.impure.write(cnt)(File.OpenOptions.default) }
        case CreateDirectory(path) => discard { path.impure.createDirectories() }
        case Copy(source, dest) => discard { source.impure.copyTo(dest.impure, overwrite = true) }
        case Move(source, dest) => discard { source.impure.moveTo(dest.impure, overwrite = true) }
      }
      result.asInstanceOf[A]
    }
  }

  def unsafePerformIO[A](program: IO[A]): A = program.foldMap(UnsafeIO)
}