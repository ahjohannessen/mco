package mco.core.general


case class Content(
  key : String,
  hash: Hash,
  kind: ContentKind = ContentKind.Garbage(),
  isInstalled: Boolean = false
)
