package mco.persistency

import cats.syntax.applicative._
import mco._
import mco.io.IOInterpreters.FSDsl._
import mco.io.Stubs
import mco.io.files.{IO, Path}
import rapture.json._
import rapture.json.jsonBackends.jawn._

class JsonStorageSpec extends UnitSpec {
  "JsonStorage#apply" should "read file and give result if provided Read value" in {
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
    for (op <- Seq(Read, Update(Vector(), Vector(1, 2, 5)))) {
      val obj = new Object
      val applyResult = storage(op)
      val (applyResult2, objResult)= storage.applyToLeft((op, obj))

      objResult should be (obj)
      runner.state(applyResult) should equal (runner.state(applyResult2))
      runner.value(applyResult) should equal (runner.value(applyResult2))
    }
  }

  "JsonStorage.Converters" should "allow to convert Package back and forth" in {
    import JsonStorage.Converters._
    for {
      kind <- Seq(ContentKind.Garbage, ContentKind.Mod, ContentKind.Doc)
      pkg = Package("pkg", Set(Content("cnt", kind, isInstalled = false)), isInstalled = false)
    } Json(pkg).as[Package] should equal (pkg)
  }

  "JsonStorage.preload" should "create repository with persistency side-effects" in {
    val repo = JsonStorage.preload("test", fakeCompanion, Path("state.json"), "target", Stubs.emptySource)
    val io = repo
      .flatMap(_.change("foo", Package("foo", Set())))
      .flatMap(_.add("bar"))
      .flatMap(_.remove("baz"))

    val state = StubIORunner(initialState).state(io)
    deepGet(Path("state.json"))(state) should equal (Some(obj("[17,25,83,1,2,3]".getBytes)))
  }

  private def fakeCompanion = new Repository.Companion[IO, Vector[Int]] {
    override def apply(key: String, s: Source[IO], t: String, state: Vector[Int]): IO[Repository[IO, Vector[Int]]] = {
      fakeRepo(state).pure[IO]
    }
  }

  private def fakeRepo(currentState: Vector[Int]): Repository[IO, Vector[Int]] =
    new Repository[IO, Vector[Int]]
    {
      override def key: String = "test"

      override def state: Vector[Int] = currentState

      override def apply(key: String): Package = fail("No package")

      override def packages: Traversable[Package] = Traversable()

      override def change(oldKey: String, updates: Package): IO[Self] =
        fakeRepo(state :+ 1).pure[IO]

      override def add(f: String): IO[Self] =
        fakeRepo(state :+ 2).pure[IO]

      override def remove(s: String): IO[Self] =
        fakeRepo(state :+ 3).pure[IO]
    }

  private def storage = new JsonStorage[Vector[Int]](Path("state.json"))

  private def initialState = fs("state.json" -> obj("[17,25,83]".getBytes))
}
