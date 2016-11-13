package mco.ui.state

import scala.util.Try

import better.files.File
import cats.instances.try_._
import cats.syntax.functor._
import mco.{Content, ContentKind, Package}

object ExecTestState extends ExecState[Try, UIState] {
  override def loadInitialState(r: UIState): UIState = r

  override def initial: Try[Vector[UIState]] = Try {
    Vector(
      UIState("repo1", Vector(
        Package("test package", Set(), isInstalled = true),
        Package("test package 2", Set(), isInstalled = false),
        Package("test package 3", Set(
          Content("readme.txt", ContentKind.Doc),
          Content("valuable.package", ContentKind.Mod),
          Content("garbage")
        ))
      )))
  }

  override def attemptRun[A](a: Try[A]): Try[A] = a

  override def react(prev: (UIState, UIState), action: UIAction): Try[(UIState, UIState)] = {
    val state = prev._1
    Try(action match {
      case SetActivePackage(_) | ClearActivePackage | InstallPackage(_) | UninstallPackage(_) =>
        (state changeBy action).getOrElse(sys.error("Unexpected failed state reaction"))
      case RenamePackage(_, _) => throw new Exception("Rename not allowed - exception test")
      case UpdateContentKind(_, _) => throw new Exception("Update not allowed - exception test")
      case AddObjects(paths, AdditionContext.Packages) =>
        val addedPackages = paths.map(p => Package(File(p).nameWithoutExtension, Set())).toSet
        val newPackages = state.packages.toSet ++ addedPackages
        UIState(state.repoName, newPackages.toVector)
      case AddObjects(_, _) => throw new Exception("Thubmnails not supported")
      case CancelPendingAdds | ReassociatePending(_, _) | SubmitPendingAdds(_) =>
        throw new Exception()
    }).fproduct(identity)
  }
}
