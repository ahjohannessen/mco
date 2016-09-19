package mco

case class Package (
  key: String,
  contents: Set[Content],
  isInstalled: Boolean = false
)

object Package extends ((String, Set[Content], Boolean) => Package) {
  implicit val packageByKeyOrd = new Ordering[Package] {
    override def compare(x: Package, y: Package): Int = x.key.compareToIgnoreCase(y.key)
  }
}