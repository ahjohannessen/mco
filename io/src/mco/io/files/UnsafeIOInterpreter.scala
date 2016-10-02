package mco.io.files

import better.files.File
import cats._
import com.olegpy.schive.Archive

object UnsafeIOInterpreter extends MonadicIO.Interp[Id] {
  override def childrenOf(path: Path): Stream[Path] =
    path.f.children.map(Path(_)).toStream

  override def descendantsOf(path: Path): Stream[Path] =
    path.f.listRecursively.map(Path(_)).toStream

  override def removeFile(path: Path): Unit = { if (path.f.exists) path.f.delete() ; () }

  override def isRegularFile(path: Path): Boolean = path.f.isRegularFile

  override def isDirectory(path: Path): Boolean = path.f.isDirectory

  override def archiveEntries(path: Path): Set[String] =
    Archive(path.f.path).files.map(_.path).toSet

  override def extract(path: Path, ft: Map[String, Path]): Unit =
    { Archive(path.f.path).extractSome(e => ft.get(e.path).map(_.f.path)); () }

  override def readBytes(path: Path): Array[Byte] = path.f.byteArray

  override def setContent(path: Path, cnt: Array[Byte]): Unit =
    { path.f.write(cnt)(File.OpenOptions.default); () }

  override def createDirectory(path: Path): Unit = { path.f.createDirectories(); () }

  override def copyTree(source: Path, dest: Path): Unit =
    { source.f.copyTo(dest.f, overwrite = true); () }

  override def moveTree(source: Path, dest: Path): Unit =
    { source.f.moveTo(dest.f, overwrite = true); () }
}
