package mco.persistency

import mco.UnitSpec
import mco.io.files.Path
import mco.io.IOInterpreters._
import FSDsl._

import rapture.json._
import jsonBackends.jawn._

class JsonStorageSpec extends UnitSpec {
  "JsonStorage#apply" should "do nothing if given NoOp value" in {
    val noop = NoOp(Vector(1, 5, 9))
    val io = storage(noop)

    val (state, result) = StubIORunner(initialState).apply(io)
    state should equal (initialState)
    result should equal (Vector(1, 5, 9))
  }

  it should "read file and give result if provided Read value" in {
    val io = storage(Read)
    val result = StubIORunner(initialState).value(io)
    result should equal (Vector(17, 25, 83))
  }

  it should "give empty state if file cannot be parsed" in {
    val state = fs("state.json" -> obj("[17,2".getBytes))
    val io = storage(Read)
    StubIORunner(state).value(io) should be (empty)
  }

  it should "write state to file given Update value" in {
    val io = storage(Update(Vector(), Vector(37, 45, 114)))
    val (state, result) = StubIORunner(initialState).apply(io)
    result should equal (Vector(37, 45, 114))
    deepGet(Path("state.json"))(state) should equal (Some(obj("[37,45,114]".getBytes)))
  }

  "JsonStorage#applyToLeft" should "use JsonStorage#apply on left element" in {
    val runner = StubIORunner(initialState)
    for (op <- Seq(Read, NoOp(Vector(1,2,3)), Update(Vector(), Vector(1, 2, 5)))) {
      val obj = new Object
      val applyResult = storage(op)
      val (applyResult2, objResult)= storage.applyToLeft((op, obj))

      objResult should be (obj)
      runner.state(applyResult) should equal (runner.state(applyResult2))
      runner.value(applyResult) should equal (runner.value(applyResult2))
    }
  }

  private def storage = new JsonStorage[Vector[Int]](Path("state.json"))

  private def initialState = fs("state.json" -> obj("[17,25,83]".getBytes))
}
