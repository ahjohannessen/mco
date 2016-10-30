package mco

sealed trait ContentKind extends Product with Serializable {
  def isInstallable: Boolean = this match {
    case ContentKind.Mod => true
    case ContentKind.Doc | ContentKind.Garbage => false
  }
}

object ContentKind {
  case object Mod     extends ContentKind
  case object Doc     extends ContentKind
  case object Garbage extends ContentKind

  def asString(kind: ContentKind): String = kind match {
    case Mod => "Mod"
    case Doc => "Doc"
    case Garbage => "Garbage"
  }
  def fromString(kind: String): Option[ContentKind] = kind match {
    case "Mod" => Some(Mod)
    case "Doc" => Some(Doc)
    case "Garbage" => Some(Garbage)
    case _ => None
  }
}
