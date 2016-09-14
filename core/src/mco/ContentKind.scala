package mco

sealed trait ContentKind {
  import ContentKind._
  def isInstallable: Boolean = this match {
    case Mod => true
    case Doc | Garbage => false
  }
}

object ContentKind {
  case object Mod     extends ContentKind
  case object Doc     extends ContentKind
  case object Garbage extends ContentKind
}
