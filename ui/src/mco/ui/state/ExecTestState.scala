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
      case SetActivePackage(p) => state.copy(currentPackage = Some(p))
      case ClearActivePackage => state.copy(currentPackage = None)
      case InstallPackage(p) => state.copy(
        packages = state.packages.map {
          case `p` => p.copy(isInstalled = true)
          case i => i
        },
        currentPackage = if (state.currentPackage contains p) Some(p.copy(isInstalled = true))
                         else state.currentPackage
      )
      case UninstallPackage(p) => state.copy(
        packages = state.packages.map {
          case `p` => p.copy(isInstalled = false)
          case i => i
        },
        currentPackage = if (state.currentPackage contains p) Some(p.copy(isInstalled = false))
                         else state.currentPackage
      )
      case RenamePackage(_, _) => throw new Exception("Rename not allowed - exception test")
      case UpdateContentKind(_, _) => throw new Exception("Update not allowed - exception test")
      case AddObjects(paths) =>
        val addedPackages = paths.map(p => Package(File(p).nameWithoutExtension, Set())).toSet
        val newPackages = state.packages.toSet ++ addedPackages
        UIState(state.repoName, newPackages.toVector)
    }).fproduct(identity)
  }
}
