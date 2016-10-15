package mco.ui.state

import mco.Package


case class UIState(
  repoName: String,
  packages: Vector[Package],
  currentPackage: Option[Package] = None
)
