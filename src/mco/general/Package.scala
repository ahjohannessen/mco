package mco.general


case class Package
(
  key: String,
  contents: Set[Content],
  isInstalled: Boolean = false
)