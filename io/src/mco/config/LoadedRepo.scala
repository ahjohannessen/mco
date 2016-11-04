package mco.config

import scala.util.Try

import cats.arrow.FunctionK
import cats.instances.try_._
import cats.instances.vector._
import cats.syntax.all._
import mco._
import mco.io.files.{IO, Path}
import mco.io.{FolderSource, IsolatedRepo}
import mco.persistency.JsonStorage
import mco.persistency.JsonStorage.Converters._
import rapture.json.jsonBackends.jawn._

object LoadedRepo {
  private def classifier(c: RepoConfig): Try[Classifier[IO]] =
    CompileAs[Classifier[IO]](c.classifier)

  private def media(c: RepoConfig): Try[Vector[Media.Companion[IO]]] =
    c.media traverse CompileAs[Media.Companion[IO]]

  private def source(c: RepoConfig): IO[Source[IO]] =
    IO.sequence(classifier(c) |@| media(c) map ((cls, ms) => FolderSource(c.source, cls, ms: _*)))
      .absorbTry

  private def repo(c: RepoConfig)(src: Source[IO]): IO[Repository[IO, Unit]] =
    (c.kind, c.persistency) match {
      case (RepoKind.Isolated, Persistency.JSON) =>
        JsonStorage.preload(c.key, IsolatedRepo, Path(c.key + ".json"), c.target, src)
          .map(new EffectRepo(_, FunctionK.id[IO]))

      case (RepoKind.Isolated, Persistency.None) =>
        IsolatedRepo(c.key, src, c.target, Set())
          .map(new EffectRepo(_, FunctionK.id[IO]))
    }

  def apply(c: RepoConfig): IO[Repository[IO, Unit]] = source(c) flatMap repo(c)
}
