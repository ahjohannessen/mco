package mco.io

import cats.{Eval, Id, ~>}
import cats.data.State
import cats.syntax.option._
import cats.syntax.traverse._
import cats.instances.list._
import mco.io.files.ops._
import mco.io.files.{MonadicIO, Path}

object IOInterpreters {
  sealed trait FileSystemObject
  case class Dir(contents: Map[String, FileSystemObject]) extends FileSystemObject
  case class Obj(data: Vector[Byte]) extends FileSystemObject
  case class Arc(dir: Dir) extends FileSystemObject

  object FSDsl {
    def fs(seq: (String, FileSystemObject)*): Dir = Dir(seq.toMap)
    def dir(seq: (String, FileSystemObject)*): FileSystemObject = Dir(seq.toMap)
    def obj(data: Vector[Byte] = Vector.empty): FileSystemObject = Obj(data)
    def obj(data: Array[Byte]): FileSystemObject = Obj(data.toVector)
    def arc(seq: (String, FileSystemObject)*): FileSystemObject = Arc(Dir(seq.toMap))

    type FStub[A] = State[Dir, A]

    def rget(prefix: String)(d: FileSystemObject): Stream[String] = d match {
      case Dir(m) =>
        m.keys.map(prefix + _).toStream #::: m
          .flatMap { case (key, fso) => rget(prefix + key + "/")(fso)}.toStream
      case _ => Stream.Empty
    }

    def deepGet(p: Path)(o: Dir): Option[FileSystemObject] =
      p
        .asString
        .split(Array('\\', '/'))
        .foldLeft((o: FileSystemObject).some) { (x, y) =>
          x match {
            case Some(Dir(m)) => m.get(y)
            case _ => none
          }
      }

    def err(p: Path): Nothing = sys.error(s"Incorrect stub fs access at ${p.asString}")

    def deepSet(p: Path, o: Option[FileSystemObject])(root: Dir): Dir = {
      def deepSetR(segments: List[String], fso: FileSystemObject): FileSystemObject = {
        (segments, fso) match {
          case (seg :: Nil, Dir(map)) => Dir(o.fold(map - seg)(map.updated(seg, _)))
          case (seg :: ss, Dir(map)) => Dir(map.updated(seg, deepSetR(ss, map(seg))))
          case _ => err(p)
        }
      }

      deepSetR(p.asString.split(Array('\\', '/')).toList, root) match {
        case d: Dir => d
        case _ => err(p)
      }
    }

    val stub = new MonadicIO.Interp[FStub] {
      override def childrenOf(path: Path): FStub[Stream[Path]] =
        State.inspect({deepGet(path) _} andThen {
          case Some(Dir(m)) =>
            m.keys.map(path / _).toStream
          case _ => err(path)
        })

      override def descendantsOf(path: Path): FStub[Stream[Path]] = State.inspect(
        deepGet(path)(_: Dir).map(rget("")).map(s => s.map(path / _)).getOrElse(Stream.Empty)
      )

      override def removeFile(path: Path): FStub[Unit] = State.modify(deepSet(path, none))

      override def isRegularFile(path: Path): FStub[Boolean] =  State.inspect({deepGet(path) _} andThen {
        case Some(Arc(_) | Obj(_)) => true
        case _ => false
      })

      override def isDirectory(path: Path): FStub[Boolean] = State.inspect({deepGet(path) _} andThen {
        case Some(Dir(_)) => true
        case _ => false
      })

      override def archiveEntries(path: Path): FStub[Set[String]] = State.inspect({deepGet(path) _} andThen {
        case Some(Arc(ch)) => rget("")(ch).toSet
        case _ => err(path)
      })

      override def extract(path: Path, ft: Map[String, Path]): FStub[Unit] =  for {
        o <- State.inspect(deepGet(path))
        contents = o.getOrElse(err(path)).asInstanceOf[Arc].dir.contents
        archived = contents.keySet
        toExtract = ft.filterKeys(archived)
        result = toExtract.map({case (k, p) => State.modify(deepSet(p, contents(k).some))})
        _ <- result.toList.sequenceU
      } yield ()

      override def readBytes(path: Path): FStub[Array[Byte]] = State.inspect({deepGet(path) _} andThen {
        case Some(Obj(data)) => data.toArray
        case _ => err(path)
      })

      override def setContent(path: Path, cnt: Array[Byte]): FStub[Unit] = State.modify(deepSet(path, obj(cnt).some))

      override def createDirectory(path: Path): FStub[Unit] = State.modify(deepSet(path, dir().some))

      override def copyTree(source: Path, dest: Path): FStub[Unit] = for {
        sourceCopy <- State.inspect(deepGet(source))
        _ <- State.modify(deepSet(dest, sourceCopy orElse err(dest)))
      } yield ()

      override def moveTree(source: Path, dest: Path): FStub[Unit] = for {
        sourceCopy <- State.inspect(deepGet(source))
        _ <- State.modify(deepSet(dest, sourceCopy orElse err(dest)))
        _ <- State.modify(deepSet(source, none))
      } yield ()
    }

    case class StubIORunner(fs: Dir) {
      def apply[A](program: IO[A]): (Dir, A) = (stub run program).run(fs).value
      def value[A](program: IO[A]): A = (stub run program).runA(fs).value
      def state[A](program: IO[A]): Dir = (stub run program).runS(fs).value
    }
  }
}
