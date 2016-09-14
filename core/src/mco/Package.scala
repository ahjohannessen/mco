package mco

case class Package
(
  key: String,
  contents: Set[Content],
  isInstalled: Boolean = false
)