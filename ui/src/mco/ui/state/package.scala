package mco.ui

import mco.{Content, ContentKind, Package}

package object state {
  def initial: Vector[UIState] = Vector(
    UIState("repo1", Seq(
      Package("test package", Set(), isInstalled = true),
      Package("test package 2", Set(), isInstalled = false),
      Package("test package 3", Set(
        Content("readme.txt", ContentKind.Doc),
        Content("valuable.package", ContentKind.Mod),
        Content("garbage")
      ))
    ))
  )

  def runAction(s: UIState, a: UIAction): UIState = a match {
    case SetActivePackage(p) => s.copy(currentPackage = Some(p))
    case InstallPackage(p) => s.copy(
      packages = s.packages.map {
        case `p` => p.copy(isInstalled = true)
        case i => i
      },
      currentPackage = if (s.currentPackage contains p) Some(p.copy(isInstalled = true)) else s.currentPackage
    )
    case UninstallPackage(p) => s.copy(
      packages = s.packages.map {
        case `p` => p.copy(isInstalled = false)
        case i => i
      },
      currentPackage = if (s.currentPackage contains p) Some(p.copy(isInstalled = false)) else s.currentPackage
    )
  }
}
