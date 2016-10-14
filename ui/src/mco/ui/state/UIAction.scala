package mco.ui.state

import mco.Package

sealed trait UIAction
case class SetActivePackage(p: Package) extends UIAction
case object ClearActivePackage          extends UIAction
case class InstallPackage(p: Package)   extends UIAction
case class UninstallPackage(p: Package) extends UIAction
