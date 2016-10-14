package mco.ui.state

import mco.Package


case class UIState(
  repoName: String,
  packages: Seq[Package],
  currentPackage: Option[Package] = None
)
