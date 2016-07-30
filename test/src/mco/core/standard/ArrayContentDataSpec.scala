package mco
package core
package standard

import better.files.File
import mco.core.general.{Content, Hash}

class ArrayContentDataSpec extends UnitSpec {
  "ArrayContentData#writeTo" should "directly output bytes into a file" in {
    forAll { bytes: Array[Byte] =>
      inTempFolder { fld =>
        val contentData = new ArrayContentData(Content("dummy", Hash(0, 0)), bytes)
        val file = File.newTemporaryFile(parent = Some(fld)).touch()

        (contentData writeTo file).io()

        file.byteArray shouldEqual bytes
      }

      ()
    }
 }
}
