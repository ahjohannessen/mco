package mco.config


import scala.util.{Failure, Success, Try}

import cats.instances.try_._
import cats.instances.vector._
import cats.syntax.traverse._
import cats.~>
import mco._
import mco.io.files.{IO, Path}
import mco.io.{FolderSource, IsolatedRepo}
import mco.persistency.JsonStorage
import monix.eval.Task
import rapture.json.jsonBackends.jawn._
import JsonStorage.serializers._

object LoadedRepo {
  def classifier(c: RepoConfig): Try[Classifier[IO]] =
    compileAs[Classifier[IO]](c.classifier)

  def media(c: RepoConfig): Try[Vector[Media.Companion[IO]]] =
    c.media traverse compileAs[Media.Companion[IO]]

  def source(c: RepoConfig): IO[Try[Source[IO]]] = {
    val t: Try[IO[Try[Source[IO]]]] = for {
      cls <- classifier(c: RepoConfig)
      ms <- media(c)
    } yield for {
      opt <- FolderSource(c.source, cls, ms: _*)
    } yield opt.fold[Try[Source[IO]]](Failure(new NoSuchElementException))(Success(_))

    IO.sequence(t) map (_ flatMap identity)
  }

  def repo(c: RepoConfig, nat: IO ~> Task)(src: Source[IO]): Task[Repository[Task, Unit]] = {
    (c.kind, c.persistency) match {
      case (RepoKind.Isolated, Persistency.JSON) =>
        JsonStorage.wrap(IsolatedRepo, Path(c.key + ".json"), c.target, src, nat)
      case (RepoKind.Isolated, Persistency.None) =>
        nat(IsolatedRepo(src, c.target, Set())) map (new EffectRepo(_, nat))
    }
  }

  def apply(c: RepoConfig)(nat: IO ~> Task): Task[Repository[Task, Unit]] =
    nat(source(c)) flatMap Task.fromTry flatMap repo(c, nat)
}
