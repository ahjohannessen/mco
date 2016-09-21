package mco.shell

import mco.io._
import mco.io.files.ops._
import mco.io.files.{Path, unsafePerformIO}
import mco.persistency.{JsonStorage, Read, Update}
import rapture.json._, jsonBackends.jawn._
import JsonStorage._

//noinspection TypeAnnotation
object Test extends App {
  val start = System.nanoTime()

  val srcIO = FolderSource("E:\\Documents\\EA Games\\The Sims 2\\__mco_instance__\\installers",
    Classifiers.enableAll, ArchiveMedia, FolderMedia).map(_.get)



  val res = for {
    src <- srcIO
    serialized = Path("./test.json")
//    _ <- setContent(serialized, "{}".getBytes)

    store = new JsonStorage[Set[mco.Package]](Path("./test.json"))

    state <- store(Read)
    bareRepo <- IsolatedRepo(
      src,
      "E:\\__tmp__\\tempdir",
      state
    )
//    repo = new PersistedRepo(bareRepo)
    _ <- store(Update(state, bareRepo.state))
  } yield ()

  println(unsafePerformIO(res))
  println(s"Took ${(System.nanoTime() - start) / 1000 / 1000}ms")
}
