package mco

case class Content(
  key : String,
  kind: ContentKind = ContentKind.Garbage,
  isInstalled: Boolean = false
)
