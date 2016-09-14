package mco.general

sealed trait ContentKind {
  import ContentKind._
  def isInstallable: Boolean = this match {
    case Mod(enabled) => enabled
    case Asset => true
    case Doc => false
    case Garbage => false
  }
}

object ContentKind {
  case class  Mod(enabled: Boolean = true) extends ContentKind
  case object Asset                        extends ContentKind
  case object Doc                          extends ContentKind
  case object Garbage                      extends ContentKind
}










