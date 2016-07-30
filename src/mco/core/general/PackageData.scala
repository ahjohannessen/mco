package mco.core.general


case class PackageData
(
  key: String,
  contents: Set[Content],
  hash: Hash,
  isInstalled: Boolean
)