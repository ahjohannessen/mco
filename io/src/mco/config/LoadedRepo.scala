package mco.config


import scala.util.{Failure, Success, Try}

import cats.instances.try_._
import cats.instances.vector._
import cats.syntax.traverse._
import cats.{Id, ~>}
import mco._
import mco.io.files.{IO, Path}
import mco.io.{EffectRepo, FolderSource, IsolatedRepo}
import mco.persistency.JsonStorage
import rapture.json.jsonBackends.jawn._
import JsonStorage.converters._

object LoadedRepo {
  private def classifier(c: RepoConfig): Try[Classifier[IO]] =
    compileAs[Classifier[IO]](c.classifier)

  private def media(c: RepoConfig): Try[Vector[Media.Companion[IO]]] =
    c.media traverse compileAs[Media.Companion[IO]]

  private def source(c: RepoConfig): IO[Try[Source[IO]]] = {
    val t: Try[IO[Try[Source[IO]]]] = for {
      cls <- classifier(c: RepoConfig)
      ms <- media(c)
    } yield for {
      opt <- FolderSource(c.source, cls, ms: _*)
    } yield opt.fold[Try[Source[IO]]](Failure(new NoSuchElementException))(Success(_))

    IO.sequence(t) map (_ flatMap identity)
  }

  private def repo(c: RepoConfig, nat: IO ~> Id)(src: Source[IO]): Repository[Id, Unit] =
    (c.kind, c.persistency) match {
      case (RepoKind.Isolated, Persistency.JSON) =>
        new EffectRepo(
          nat(JsonStorage.preload(IsolatedRepo, Path(c.key + ".json"), c.target, src)),
          nat
        )

      case (RepoKind.Isolated, Persistency.None) =>
        new EffectRepo(
          nat(IsolatedRepo(src, c.target, Set())),
          nat
        )
    }

  def apply(c: RepoConfig)(nat: IO ~> Id): Try[Repository[Id, Unit]] =
    nat(source(c)) map repo(c, nat)
}
