package mco.ui.state

import java.net.URL

import mco.Package
import cats.syntax.option._

case class UIState(
  repoName: String,
  packages: Vector[Package],
  currentPackage: Option[Package] = None,
  thumbnailURL: Option[URL] = None
) {
  def changeBy(action: UIAction): Option[UIState] = action match {
    case SetActivePackage(p) => copy(currentPackage = Some(p)).some
    case ClearActivePackage => copy(currentPackage = None, thumbnailURL = None).some
    case InstallPackage(p) => copyFn(p.key, _.copy(isInstalled = true)).some
    case UninstallPackage(p) => copyFn(p.key, _.copy(isInstalled = false)).some
    case RenamePackage(from, to) => copyFn(from, _.copy(key = to)).some
    case UpdateContentKind(contentKey, kind) =>
      currentPackage.fold(this)(p =>
        copyFn(p.key, _ => p.copy(contents = p.contents.map {
          case c if c.key == contentKey => c.copy(kind = kind)
          case c => c
        }))
      ).some
    case AddObjects(_) => none
  }

  private def updateFn(key: String, f: Package => Package)(pkg: Package) = pkg match {
    case affected if affected.key == key => f(affected)
    case _ => pkg
  }

  private def copyFn(key: String, f: Package => Package) = copy(
    packages = packages.map(updateFn(key, f)),
    currentPackage = currentPackage.map(updateFn(key, f))
  )
}
