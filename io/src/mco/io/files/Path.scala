package mco.io.files

import java.net.URL

import better.files.File


case class Path(str: String) {
  val normalized: String = str.replace('\\', '/').stripSuffix("/")
  private[io] def f: File = File(str)
  def relativeToS(other: Path): String = other.f.relativize(f).toString.replace('\\', '/')
  def asString: String = str
  def fileName: String = normalized.drop(normalized.lastIndexOf("/") + 1)
  def extension: Option[String] = {
    val dotIndex = fileName lastIndexOf '.'
    if (dotIndex != -1) Some(fileName.drop(dotIndex + 1))
    else None
  }

  def parent: Path = {
    val li = normalized lastIndexOf '/'
    if (li > 0) Path(normalized take (li + 1))
    else Path("")
  }

  def /(right: String): Path = Path(s"$normalized/$right")
  def toURL: URL = f.toJava.toURI.toURL

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
