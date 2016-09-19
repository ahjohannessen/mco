package mco

class PackageSpec extends UnitSpec {
  "Package Ordering" should "sort packages by key" in {
    val values = Vector(
      Package("Ohm", Set()),
      Package("Zod", Set()),
      Package("Amn", Set())
    )

    values.sorted.map(_.key) shouldEqual Vector("Amn", "Ohm", "Zod")
  }
}
