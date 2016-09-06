package mco.general

sealed trait ContentKind {
  import ContentKind._
  def isInstallable: Boolean = this match {
    case Mod(enabled) => enabled
    case Asset => true
    case Doc => false
    case Garbage(ov) => ov.exists(_.isInstallable)
  }
}

object ContentKind {
  case class  Mod(enabled: Boolean = true)                    extends ContentKind
  case object Asset                                           extends ContentKind
  case object Doc                                             extends ContentKind
  case class  Garbage(`override`: Option[ContentKind] = None) extends ContentKind {
    require(`override` collect { case g: Garbage => false } getOrElse true,
      "Garbage cannot be overridden with garbage")
  }
}










