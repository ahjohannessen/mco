package mco.io

import scala.util.Failure

import mco.{Fail, UnitSpec}
import mco.io.IOInterpreters.FSDsl._
import mco.io.files._

class IOSpec extends UnitSpec {
  "IO#absorbTry" should "convert non-Fail exception to Fail.Uncaught" in {
    a [Fail.Uncaught] shouldBe thrownBy {
      run(IO.pure(Failure(new Exception("Fail"))).absorbTry)
    }
  }

  it should "leave Fail exceptions as they are" in {
    a [Fail.MissingResource] shouldBe thrownBy {
      run(IO.pure(Failure(Fail.MissingResource("foo"))).absorbTry)
    }
  }

  def run[A](io: IO[A]): A = StubIORunner(fs()).value(io)
}
