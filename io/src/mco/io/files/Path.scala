package mco.io.files

import better.files.File


case class Path(str: String) {
  val normalized: String = str.replace('\\', '/')
  private[io] def f: File = File(str)
  def relativeToS(other: Path): String = other.f.relativize(f).toString.replace('\\', '/')
  def asString: String = str
  def fileName: String = normalized.drop(normalized.lastIndexOf("/") + 1)
  def /(right: String): Path = Path(s"$normalized/$right")

  // $COVERAGE-OFF$Equals-hashcode is by contract, even though it's not used in all cases
  override def equals(other: Any): Boolean = other match {
    case p: Path => normalized == p.normalized
    case _ => false
  }

  override def hashCode(): Int = normalized.hashCode()
  // $COVERAGE-ON$
}

object Path extends (String => Path) {
  def apply(s: File): Path = apply(s.pathAsString)
}
