package mco

import better.files.File
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, LoneElement, Matchers}

trait UnitSpec extends FlatSpec
  with Matchers
  with LoneElement
  with GeneratorDrivenPropertyChecks {

  def inTempFolder(body: File => Unit) = {
    val tmp = File.newTemporaryDirectory("mco-unit-tests-")
    body(tmp)
    tmp.delete(true)
  }
}
