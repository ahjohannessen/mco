package mco
package core
package standard

import mco.core.general.Hash

class UtilsSpec extends UnitSpec {
  "Utils#hashFile" should "generate Hash object for known files" in {
    inTempFolder { fld =>
      val f1 = fld / "file1"
      f1.touch().write("Hello")
      (Utils hashFile f1).io() shouldBe Hash(753694413698530628L,-3042045079152025465L)

      val f2 = fld / "file2"
      f2.touch().write("World")
      (Utils hashFile f2).io() shouldBe Hash(1846988464401551951L,-138626083409739144L)

      val f3 = fld / "file3"
      f3.touch().write(", !")
      (Utils hashFile f3).io() shouldBe Hash(-1878249218591150391L,4344127366426008665L)
    }
  }
}
