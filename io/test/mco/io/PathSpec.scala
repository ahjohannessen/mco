package mco.io

import mco.UnitSpec
import mco.io.files.Path

class PathSpec extends UnitSpec {
  "Path#extension" should "return last suffix of file without dot, if it exists" in {
    Path("dir/file.first.second.ext").extension shouldBe Some("ext")
    Path("dir/file").extension shouldBe empty
  }

  "Path#parent" should "return parent of the path" in {
    Path("dir/subdir").parent shouldEqual Path("dir")
    Path("/").parent shouldEqual Path("")
    Path("foo").parent shouldEqual Path("")
  }
}
