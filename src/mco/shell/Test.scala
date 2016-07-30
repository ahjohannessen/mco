package mco.shell

import better.files.File
import mco.core.general.Repository
import mco.core.monad.IO
import mco.core.standard._
import net.sf.sevenzipjbinding.SevenZip

object Test extends App {
//  val src = FolderSource(
//    File("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers"),
//    ArchiveMedia, FolderMedia
//  )
//  val Repo = new Repository(
//    new FolderTarget(File("E:\\__tmp__\\tempdir")),
//    IO.unsafePerform(SimplePackages(src))
//  )
  val io = for {
    sourceOption <- FolderSource(
      File("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers"),
      ArchiveMedia, FolderMedia
    )
    packages <- SimplePackages(sourceOption.get)
    repo = new Repository(new IsolatedTarget(File("E:\\__tmp__\\tempdir")), packages)
    installed <- repo.install("62-Bet on it.zip")
  } yield installed

//  val printio = io.map(r => r.all mkString "\n").map(println)

  val start = System.nanoTime()
  IO.unsafePerform(io)
  println("END: ", System.nanoTime() - start)
}
