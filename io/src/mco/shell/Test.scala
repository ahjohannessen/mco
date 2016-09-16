package mco.shell

import mco.io.files.{IO, Path, unsafePerformIO}
import cats.syntax.foldable._
import cats.instances.list._
import mco.Repository
import mco.io._

//noinspection TypeAnnotation
object Test extends App {
  val start = System.nanoTime()

  val srcIO = FolderSource("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers",
    Classifiers.enableAll, ArchiveMedia, FolderMedia).map(_.get)

  val res = for {
    src <- srcIO
    repo <- IsolatedRepo(
      src,
      Path("E:\\__tmp__\\tempdir"),
      ()
    )
    pkgs = repo.packages.slice(0, 40).map(_.key).toList
    _ = println(repo.packages.slice(0, 40).map(_.isInstalled))
    rr <- pkgs.foldM[IO, Repository.Aux[IO, Path, Unit]](repo)((repo, key) => repo.change(key, repo(key).copy(isInstalled = true)))
  } yield rr.packages.slice(0, 40).map(_.isInstalled).toList

  println(unsafePerformIO(res))
  println(s"Took ${(System.nanoTime() - start) / 1000 / 1000}ms")
}
