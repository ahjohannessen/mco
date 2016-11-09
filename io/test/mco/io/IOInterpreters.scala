package mco.io

import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.time.Instant

import scala.concurrent.duration._

import cats.data.State
import cats.syntax.option._
import cats.syntax.traverse._
import cats.instances.list._
import mco.io.files._

object IOInterpreters {
  sealed trait FileSystemObject
  case class Dir(contents: Map[String, FileSystemObject]) extends FileSystemObject
  case class Obj(data: Vector[Byte], dt: FiniteDuration) extends FileSystemObject
  case class Arc(dir: Dir) extends FileSystemObject

  object FSDsl {

    implicit class StubIOSyntax[A](val self: IO[A]) {
      def on(fs: Dir): (Dir, A) = StubIORunner(fs)(self)
    }

    def fs(seq: (String, FileSystemObject)*): Dir = Dir(seq.toMap)
    def dir(seq: (String, FileSystemObject)*): FileSystemObject = Dir(seq.toMap)
    def obj(data: Vector[Byte] = Vector.empty, offset: FiniteDuration = 0.second) =
      Obj(data, offset)

    def obj(data: Array[Byte]): FileSystemObject = Obj(data.toVector, 0.second)
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
            case d @ Some(Dir(m)) => if (y.isEmpty) d else m.get(y)
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

      deepSetR(p.asString.split(Array('\\', '/')).toList.filter(_.nonEmpty), root) match {
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
        case Some(Arc(_) | Obj(_, _)) => true
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
        case Some(Obj(data, _)) => data.toArray
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

      override def stat(path: Path): FStub[BasicFileAttributes] = for {
        objOpt <- State.inspect(deepGet(path))
        obj = objOpt.getOrElse(err(path))
        isDir <- isDirectory(path)
        isFile <- isRegularFile(path)
        defaultTime = FileTime.from(Instant.parse("2007-12-03T10:15:30.00Z"))
        time = obj match {
          case Obj(_, t) => FileTime.fromMillis(defaultTime.toMillis + t.toMillis)
          case _ => defaultTime
        }
      } yield new BasicFileAttributes {
        override def fileKey(): FileSystemObject = obj
        override def isRegularFile: Boolean = isFile
        override def isOther: Boolean = false
        override def lastModifiedTime(): FileTime = time
        override def size(): Long = obj match {
          case Dir(_) => 0L
          case Arc(_) => 18414L
          case Obj(v, _) => v.length.toLong
        }
        override def isDirectory: Boolean = isDir
        override def isSymbolicLink = false
        override def creationTime(): FileTime = time
        override def lastAccessTime(): FileTime = time
      } : BasicFileAttributes
    }

    case class StubIORunner(fs: Dir) {
      def apply[A](program: IO[A]): (Dir, A) = program.unsafePerformWith(stub).run(fs).value
      def value[A](program: IO[A]): A = program.unsafePerformWith(stub).runA(fs).value
      def state[A](program: IO[A]): Dir = program.unsafePerformWith(stub).runS(fs).value
    }
  }
}
