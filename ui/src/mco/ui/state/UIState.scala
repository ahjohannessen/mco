package mco.ui.state

import mco.Package


case class UIState(
  repoName: String,
  packages: Vector[Package],
  currentPackage: Option[Package] = None
) {
  def changeBy(action: UIAction): UIState = action match {
    case SetActivePackage(p) => copy(currentPackage = Some(p))
    case ClearActivePackage => copy(currentPackage = None)
    case InstallPackage(p) => copyFn(p.key, _.copy(isInstalled = true))
    case UninstallPackage(p) => copyFn(p.key, _.copy(isInstalled = false))
    case RenamePackage(from, to) => copyFn(from, _.copy(key = to))
    case UpdateContentKind(contentKey, kind) =>
      currentPackage.fold(this)(p =>
        copyFn(p.key, _ => p.copy(contents = p.contents.map {
          case c if c.key == contentKey => c.copy(kind = kind)
          case c => c
        }))
      )
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
