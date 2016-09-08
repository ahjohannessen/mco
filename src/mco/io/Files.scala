package mco.io

import better.files.File
import cats.{Monad, Id, ~>}
import cats.free.Free
import cats.free.Free.liftF
import com.olegpy.schive.Archive

object Files {
  case class Path(str: String) {
    val normalized = str.replace('\\', '/')
    def impure = File(str)
    def relativeToS(other: Path) = other.impure.relativize(impure).toString
    def asString = str
    def fileName = normalized.drop(normalized.indexOf("/") + 1)
    def /(right: String) = Path(s"$normalized/$right")

    override def equals(other: Any): Boolean = other match {
      case p: Path => normalized == p.normalized
      case _ => false
    }

    override def hashCode(): Int = normalized.hashCode()
  }

  object Path extends (String => Path) {
    def apply(s: File): Path = apply(s.pathAsString)
  }

  object Ast {
    sealed trait FileOperationA[A]
    case class ChildrenOf(path: Path)                        extends FileOperationA[Stream[Path]]
    case class DescendantsOf(path: Path)                     extends FileOperationA[Stream[Path]]
    case class RemoveFile(path: Path)                        extends FileOperationA[Unit]
    case class IsRegularFile(path: Path)                     extends FileOperationA[Boolean]
    case class IsDirectory(path: Path)                       extends FileOperationA[Boolean]
    case class ArchiveEntries(path: Path)                    extends FileOperationA[Set[String]]
    case class Extract(archive: Path, ft: Map[String, Path]) extends FileOperationA[Unit]
    case class ReadBytes(path: Path)                         extends FileOperationA[Array[Byte]]
    case class SetContent(path: Path, cnt: Array[Byte])      extends FileOperationA[Unit]
    case class CreateDirectory(path: Path)                   extends FileOperationA[Unit]
    case class Copy(source: Path, dest: Path)                extends FileOperationA[Unit]
    case class Move(source: Path, dest: Path)                extends FileOperationA[Unit]
  }

  import Ast._

  type IO[A] = Free[FileOperationA, A]
  implicit val IO: Monad[IO] = Free.catsFreeMonadForFree[FileOperationA]

  def childrenOf(path: Path)                     : IO[Stream[Path]] = liftF(ChildrenOf(path))
  def descendantsOf(path: Path)                  : IO[Stream[Path]] = liftF(DescendantsOf(path))
  def removeFile(path: Path)                     : IO[Unit]         = liftF(RemoveFile(path))
  def isRegularFile(path: Path)                  : IO[Boolean]      = liftF(IsRegularFile(path))
  def isDirectory(path: Path)                    : IO[Boolean]      = liftF(IsDirectory(path))
  def archiveEntries(path: Path)                 : IO[Set[String]]  = liftF(ArchiveEntries(path))
  def extract(path: Path, ft: Map[String, Path]) : IO[Unit]         = liftF(Extract(path, ft))
  def readBytes(path: Path)                      : IO[Array[Byte]]  = liftF(ReadBytes(path))
  def setContent(path: Path, cnt: Array[Byte])   : IO[Unit]         = liftF(SetContent(path, cnt))
  def createDirectory(path: Path)                : IO[Unit]         = liftF(CreateDirectory(path))
  def copy(source: Path, dest: Path)             : IO[Unit]         = liftF(Copy(source, dest))
  def move(source: Path, dest: Path)             : IO[Unit]         = liftF(Move(source, dest))

  @inline final def discard[A](a: => A): Unit = { a; () }


  def unsafePerformIO[A](program: IO[A]): A = program.foldMap(new (FileOperationA ~> Id) {
    override def apply[B](fa: FileOperationA[B]): Id[B] = {
      val result = fa match {
        case ChildrenOf(path) => path.impure.children.map(Path(_)).toStream
        case DescendantsOf(path) => path.impure.listRecursively.map(Path(_)).toStream
        case RemoveFile(path) => discard { if (path.impure.exists) path.impure.delete() }
        case IsRegularFile(path) => path.impure.isRegularFile
        case IsDirectory(path) => path.impure.isDirectory
        case ArchiveEntries(path) => Archive(path.impure.path).files.map(_.path).toSet
        case Extract(path, ft) =>
          Archive(path.impure.path).extractSome(e => ft.get(e.path).map(_.impure.path))
        case ReadBytes(path) => path.impure.byteArray
        case SetContent(path, cnt) => discard { path.impure.write(cnt)(File.OpenOptions.default) }
        case CreateDirectory(path) => discard { path.impure.createDirectories() }
        case Copy(source, dest) => discard { source.impure.copyTo(dest.impure, overwrite = true) }
        case Move(source, dest) => discard { source.impure.moveTo(dest.impure, overwrite = true) }
      }
      result.asInstanceOf[B]
    }
  })
}