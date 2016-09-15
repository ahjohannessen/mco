package mco

sealed trait ContentKind {
  def isInstallable: Boolean = this match {
    case ContentKind.Mod => true
    case ContentKind.Doc | ContentKind.Garbage => false
  }
}

object ContentKind {
  case object Mod     extends ContentKind
  case object Doc     extends ContentKind
  case object Garbage extends ContentKind
}
