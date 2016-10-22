package mco.ui.state

import scala.util.Try

import cats.arrow.FunctionK
import cats.instances.vector._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.traverse._
import com.typesafe.config.ConfigFactory
import mco._
import mco.config.{LoadedRepo, RepoConfigs}
import mco.io.files._

object ExecIOState extends ExecState[IO, Repository[IO, Unit]] {

  override def attemptRun[A](a: IO[A]): Try[A] = Try { a unsafePerformWith UnsafeIOInterpreter }

  override def initial: IO[Vector[Repository[IO, Unit]]] = {
    val loadConfigTry = for {
      configBytes <- readBytes(Path("mco.conf"))
      config = ConfigFactory.parseString(new String(configBytes))
      repoConfigTry = RepoConfigs(config)
    } yield repoConfigTry

    for {
      configs <- loadConfigTry.absorbTry
      repos <- configs.toVector.traverse(LoadedRepo(_, FunctionK.id[IO]))
    } yield repos
  }

  override def react(prev: (Repository[IO, Unit], UIState), action: UIAction): IO[(Repository[IO, Unit], UIState)] = {
    val (repo, state) = prev
    val updatedRepo = action match {
      case SetActivePackage(p) => repo.pure[IO]
      case ClearActivePackage => repo.pure[IO]
      case InstallPackage(p) =>
        repo.change(p.key, _.copy(isInstalled = true))
      case UninstallPackage(p) =>
        repo.change(p.key, _.copy(isInstalled = false))
      case RenamePackage(from, to) =>
        repo.change(from, _.copy(key = to))
      case UpdateContentKind(contentKey, kind) =>
        state.currentPackage
          .fold(repo.pure[IO])(pkg => repo.change(pkg.key, existing =>
            existing.copy(contents = existing.contents.map {
              case c if c.key == contentKey => c.copy(kind = kind)
              case c => c
            })))

    }
    updatedRepo.fproduct(_ => state changeBy action)
  }

  override def loadInitialState(r: Repository[IO, Unit]): UIState = UIState(
    r.key,
    r.packages.toVector
  )
}
