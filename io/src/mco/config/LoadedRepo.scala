package mco.config


import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

import cats.instances.try_._
import cats.instances.vector._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import cats.{Monad, Functor, ~>}
import mco._
import mco.io.files.{IO, Path}
import mco.io.{EffectRepo, FolderSource, IsolatedRepo}
import mco.persistency.JsonStorage
import mco.persistency.JsonStorage.Converters._
import rapture.json.jsonBackends.jawn._

object LoadedRepo {
  private def classifier(c: RepoConfig): Try[Classifier[IO]] =
    CompileAs[Classifier[IO]](c.classifier)

  private def media(c: RepoConfig): Try[Vector[Media.Companion[IO]]] =
    c.media traverse CompileAs[Media.Companion[IO]]

  private def source(c: RepoConfig): IO[Try[Source[IO]]] = {
    val t: Try[IO[Try[Source[IO]]]] = for {
      cls <- classifier(c: RepoConfig)
      ms <- media(c)
    } yield for {
      opt <- FolderSource(c.source, cls, ms: _*)
    } yield opt.fold[Try[Source[IO]]](Failure(new NoSuchElementException))(Success(_))

    IO.sequence(t) map (_ flatMap identity)
  }

  private def repo[F[_]: Functor](c: RepoConfig, nat: IO ~> F)(src: Source[IO]): F[Repository[F, Unit]] =
    (c.kind, c.persistency) match {
      case (RepoKind.Isolated, Persistency.JSON) =>
        nat(JsonStorage.preload(IsolatedRepo, Path(c.key + ".json"), c.target, src))
          .map(new EffectRepo(_, nat))

      case (RepoKind.Isolated, Persistency.None) =>
        nat(IsolatedRepo(src, c.target, Set()))
          .map(new EffectRepo(_, nat))
    }

  def apply[F[_]: Monad](c: RepoConfig, nat: IO ~> F): F[Try[Repository[F, Unit]]] = {
    nat(source(c)) map (_ map repo(c, nat)) flatMap (x => x.sequence)
  }
}
