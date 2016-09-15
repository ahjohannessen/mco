package mco

import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

trait UnitSpec extends FlatSpec
  with Matchers
  with LoneElement
  with GeneratorDrivenPropertyChecks