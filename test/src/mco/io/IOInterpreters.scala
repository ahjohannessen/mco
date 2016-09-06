package mco.io

import java.nio.file.Paths

import cats.data.State
import cats.{Id, ~>}
import com.olegpy.schive.Archive
import mco.io.Files.Ast._
import mco.io.Files._

object IOInterpreters {
  type IOLastOp[A] = State[FileOperationA[_], A]

  def empties[R] = new (Ast.FileOperationA ~> Id) {
    override def apply[A](fa: FileOperationA[A]): Id[A] = {
      val res = fa match {
        case ChildrenOf(path) => Stream.Empty
        case DescendantsOf(path) => Stream.Empty
        case RemoveFile(path) => ()
        case IsRegularFile(path) => false
        case IsDirectory(path) => false
        case ReadBytes(path) => Array.emptyByteArray
        case SetContent(path, cnt) => ()
        case CreateDirectory(path) => ()
        case Copy(source, dest) => ()
        case Move(source, dest) => ()
        case ArchiveEntries(path) => Stream.Empty
        case Extract(archive, ft) => ()
      }
      res.asInstanceOf[A]
    }
  }

  def lastOperation[A](io: IO[A]) = io.foldMap(new (Ast.FileOperationA ~> IOLastOp) {
    override def apply[B](fa: FileOperationA[B]): IOLastOp[B] = State(_ => (fa, empties(fa)))
  }).runS(null).value
}
