package mco

import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

trait UnitSpec extends FlatSpec
  with Matchers
  with LoneElement
  with GeneratorDrivenPropertyChecks {

//  def inTempFolder(body: File => Unit) = {
//    val tmp = File.newTemporaryDirectory("mco-unit-tests-")
//    body(tmp)
//    tmp.delete(true)
//  }
}
