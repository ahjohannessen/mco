package mco.shell

import mco.general.Repository
import mco.io.Files._
import mco.io._

object Test extends App {
//  val src = FolderSource(
//    File("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers"),
//    ArchiveMedia, FolderMedia
//  )
//  val Repo = new Repository(
//    new FolderTarget(File("E:\\__tmp__\\tempdir")),
//    IO.unsafePerform(SimplePackages(src))
//  )
//  val io = for {
//    sourceOption <- FolderSource(
//      File("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers"),
//      ArchiveMedia, FolderMedia
//    )
//    packages <- SimplePackages(sourceOption.get)
//    repo = new Repository(new IsolatedTarget(File("E:\\__tmp__\\tempdir")), packages)
//    installed <- repo.install("62-Bet on it.zip")
//  } yield installed
//
////  val printio = io.map(r => r.all mkString "\n").map(println)
//
//  IO.unsafePerform(io)

  val start = System.nanoTime()

  val srcIO = FolderSource("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers",
    Classifiers.customizable, ArchiveMedia, FolderMedia).map(_.get)

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
