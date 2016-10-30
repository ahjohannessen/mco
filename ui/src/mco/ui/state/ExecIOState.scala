package mco.ui.state

import scala.util.Try

import cats.arrow.FunctionK
import cats.instances.vector._
import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import mco._
import mco.config.{LoadedRepo, RepoConfigs}
import mco.io.files._
import mco.ui.state.PipeSyntax._

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

    def withNewState(updated: IO[Repository[IO, Unit]]) = {
      updated
        .fproduct(r => state changeBy action getOrElse loadInitialState(r).copy(
          currentPackage = state.currentPackage,
          thumbnailURL = state.thumbnailURL
        ))
    }

    action match {
      case SetActivePackage(p) =>
        val url = attemptRun(repo.thumbnail(p.key)).toOption.flatten
        val nextState = state.copy(currentPackage = p.some, thumbnailURL = url)
        (repo, nextState).pure[IO]
      case ClearActivePackage => repo.pure[IO] |> withNewState
      case InstallPackage(p) =>
        repo.change(p.key, _.copy(isInstalled = true)) |> withNewState
      case UninstallPackage(p) =>
        repo.change(p.key, _.copy(isInstalled = false)) |> withNewState
      case RenamePackage(from, to) =>
        repo.change(from, _.copy(key = to)) |> withNewState
      case UpdateContentKind(contentKey, kind) =>
        state.currentPackage
          .fold(repo.pure[IO])(pkg => repo.change(pkg.key, existing =>
            existing.copy(contents = existing.contents.map {
              case c if c.key == contentKey => c.copy(kind = kind)
              case c => c
            }))) |> withNewState
      case AddObjects(paths) =>
        paths.foldM[IO, Repository[IO, Unit]](repo)(_ add _) |> withNewState

    }
  }

  override def loadInitialState(r: Repository[IO, Unit]): UIState = {
    UIState(r.key, r.packages.toVector)
  }
}