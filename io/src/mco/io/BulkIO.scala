package mco.io

import mco.Repository

import java.nio.file.attribute.FileTime

import cats.implicits._
import mco.io.files._

object BulkIO {
  case class ClassifiedPaths(packages: Vector[Path], images: Vector[Path])

  def classify(paths: Vector[Path])
               (repo: Repository[IO, _]): IO[ClassifiedPaths] =
    for {
      ordered <- sortByLastModified(paths)
      images  <- ordered.filterA[IO](isImage)
      pkgs    <- ordered.filterA[IO](isImage(_) map negate)
      invalid <-    pkgs.filterA[IO](repo canAdd _.asString map negate)
      _       <- Fail.MultipleFailures(
        invalid.map(p => Fail.UnexpectedType(p.asString, "a valid package")): _*
      ) when invalid.nonEmpty
    } yield ClassifiedPaths(pkgs, images)

  def add[S](assoc: Map[Path, Option[Path]])(repo: Repository[IO, S]): IO[Repository[IO, S]] = {
    assoc.toVector.foldM[IO, Repository[IO, S]](repo)((repo, kv) => kv match {
      case (pkgPath, None) => repo add_ pkgPath.asString
      case (pkgPath, Some(imgPath)) => addWithThumbnail(repo, pkgPath, imgPath)
    })
  }

  private def addWithThumbnail[S](repo: Repository[IO, S], pkgPath: Path, imgPath: Path
                                 ): IO[Repository[IO, S]] =
    for {
      addResult <- repo add pkgPath.asString
      (newPackage, newRepo) = addResult
      _ <- newRepo.thumbnail(newPackage.key) setFrom imgPath.asString
    } yield newRepo

  private def negate(b: Boolean): Boolean = !b

  private def sortByLastModified(paths: Vector[Path]): IO[Vector[Path]] =
    paths
      .map(p => stat(p) map (p -> _.lastModifiedTime))
      .sequence[IO, (Path, FileTime)]
      .map(paths sortBy _.toMap)

  private def isImage(path: Path): IO[Boolean] =
    for (isFile <- isRegularFile(path))
      yield isFile && path.extension.exists(ImageExtensions contains _)
}
