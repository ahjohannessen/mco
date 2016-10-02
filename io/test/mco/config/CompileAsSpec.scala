package mco.config

import scala.util.Success

import mco.UnitSpec
import org.scalatest.tagobjects.Slow

class CompileAsSpec extends UnitSpec {
  "compileAs" should "retrieve object field if it is of specified type" taggedAs Slow in {
    compileAs[List.type]("List") shouldBe Success(List)
  }

  it should "retrieve object if it's of more generic type" taggedAs Slow in {
    compileAs[Any]("List") shouldBe Success(List)
  }

  it should "not retrieve object if supplied type is wrong" taggedAs Slow in {
    compileAs[List.type]("Vector") should be a 'failure
  }

  it should "not allow chaining multiple elements" taggedAs Slow in {
    compileAs[Any]("Vector; List") should be a 'failure
  }

  it should "not allow calling functions" taggedAs Slow in {
    compileAs[Any]("System.exit(1)") should be a 'failure
  }
}
