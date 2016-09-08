package mco.io

import cats.data.State
import cats.{Id, ~>}
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

  sealed trait FileSystemObject
  case class Dir(contents: Map[String, FileSystemObject]) extends FileSystemObject
  case class Obj(data: Array[Byte]) extends FileSystemObject
  case class Arc(dir: Dir) extends FileSystemObject

  object FSDsl {
    def fs(seq: (String, FileSystemObject)*): Dir = Dir(seq.toMap)
    def dir(seq: (String, FileSystemObject)*): FileSystemObject = Dir(seq.toMap)
    def obj(data: Array[Byte] = Array.emptyByteArray): FileSystemObject = Obj(data)
    def arc(seq: (String, FileSystemObject)*): FileSystemObject = Arc(Dir(seq.toMap))

    type FStub[A] = State[Dir, A]

    def deepGet(p: Path)(o: Dir) = p.asString.split(Array('\\', '/')).foldLeft((o: FileSystemObject).some) { (x, y) =>
      x match {
        case Some(Dir(m)) => m.get(y)
        case _ => none
      }
    }

    def err = sys.error("Incorrect stub fs access")

    def deepSet(p: Path, o: Option[FileSystemObject])(root: Dir): Dir = {
      def deepSetR(segments: List[String], fso: FileSystemObject): FileSystemObject = {
        (segments, fso) match {
          case (seg :: Nil, Dir(map)) => Dir(o.fold(map - seg)(map.updated(seg, _)))
          case (seg :: ss, Dir(map)) => Dir(map.updated(seg, deepSetR(ss, map(seg))))
          case _ => err
        }
      }

      deepSetR(p.asString.split(Array('\\', '/')).toList, root) match {
        case d: Dir => d
        case _ => err
      }
    }

    val stub = new (FileOperationA ~> FStub) {
      def rget(d: FileSystemObject): Stream[Path] = d match {
        case Dir(m) => m.keys.map(Path(_)).toStream #::: m.values.flatMap(rget).toStream
        case _ => Stream.Empty
      }

      override def apply[A](fa: FileOperationA[A]): FStub[A] = {
        val res = fa match {
          case ChildrenOf(path) => State.inspect({deepGet(path) _} andThen {
            case Some(Dir(m)) =>
              m.keys.map(path / _).toStream
            case _ => err
          })
          case DescendantsOf(path) =>
            State.inspect(deepGet(path)(_: Dir).map(rget).getOrElse(Stream.Empty))

          case RemoveFile(path) => State.modify(deepSet(path, none))

          case IsRegularFile(path) => State.inspect({deepGet(path) _} andThen {
            case Some(Arc(_) | Obj(_)) => true
            case _ => false
          })
          case IsDirectory(path) => State.inspect({deepGet(path) _} andThen {
            case Some(Dir(_)) => true
            case _ => false
          })
          case ArchiveEntries(path) => State.inspect({deepGet(path) _} andThen {
            case Some(Arc(ch)) => rget(ch).map(_.asString).toSet
            case _ => err
          })
          case Extract(archive, ft) => for {
            o <- State.inspect(deepGet(archive))
            contents = o.get.asInstanceOf[Arc].dir.contents
            archived = contents.keySet
            toExtract = ft.filterKeys(archived)
            result = toExtract.map({case (k, p) => State.modify(deepSet(p, contents(k).some))})
            _ <- result.toList.sequenceU
          } yield ()
          case ReadBytes(path) => State.inspect({deepGet(path) _} andThen {
            case Some(Obj(data)) => data
            case _ => err
          })
          case SetContent(path, cnt) => State.modify(deepSet(path, obj(cnt).some))
          case CreateDirectory(path) => State.modify(deepSet(path, dir().some))
          case Copy(source, dest) => for {
            sourceCopy <- State.inspect(deepGet(source))
            _ <- State.modify(deepSet(dest, sourceCopy.get.some))
          } yield ()

          case Move(source, dest) => for {
            sourceCopy <- State.inspect(deepGet(source))
            _ <- State.modify(deepSet(dest, sourceCopy.get.some))
            _ <- State.modify(deepSet(source, none))
          } yield ()
        }
        res.asInstanceOf[FStub[A]]
      }
    }

    case class StubRun(fs: Dir) {
      def apply[A](fa: IO[A]) = fa.foldMap(stub).run(fs).value
      def value[A](fa: IO[A]) = fa.foldMap(stub).runA(fs).value
      def state[A](fa: IO[A]) = fa.foldMap(stub).runS(fs).value
    }
  }
}
