package mco.persistency

import scala.util.Try

import alleycats.Empty
import cats.{Id, ~>}
import cats.instances.tuple._
import cats.syntax.bifunctor._
import cats.syntax.applicative._
import cats.syntax.functor._
import mco._
import mco.io.EffectRepo
import mco.io.files.ops._
import mco.io.files.{IO, Path}
import rapture.core.modes.throwExceptions._
import rapture.json._
import rapture.json.jsonBackends.jawn._

class JsonStorage[S: Serializer[?, Json]: Extractor[?, Json]: Empty](target: Path)
  extends (StoreOp[S] => IO[S]) {

  override def apply(v1: StoreOp[S]): IO[S] = v1 match {
    case Update(_, next) => setContent(target, Json(next).toBareString.getBytes("UTF-8")) as next
    case Read => readBytes(target)
      .map(new String(_))
      .map(s => Try(Json.parse(s).as[S]) getOrElse Empty[S].empty)
    case NoOp(s) => IO.pure(s)
  }

  def applyToLeft[A]: ((StoreOp[S], A)) => (IO[S], A) = _ leftMap this
}

object JsonStorage {
  def wrap[S: Serializer[?, Json]: Extractor[?, Json]: Empty]
  (
    repo: Repository.Companion[IO, S],
    json: Path,
    target: String,
    source: Source[IO],
    nat: IO ~> Id
  ): Repository[Id, Unit] = nat {
    for {
      storage <- new JsonStorage[S](json).pure[IO]
      state <- storage(Read)
      loaded <- repo(source, target, state)
    } yield new EffectRepo[PersistedRepo.Lift[IO, S]#T, Unit](
      new PersistedRepo(loaded).widen,
      outputState(storage) andThen nat
    )
  }

  private def outputState[S](st: JsonStorage[S]) = new (PersistedRepo.Lift[IO, S]#T ~> IO) {
    override def apply[A](fa: IO[(StoreOp[S], A)]): IO[A]
      = fa map st.applyToLeft[A] flatMap { case (io, a) => io as a }
  }

  object converters {
    import ContentKind._
    implicit val contentKindExtractor: Extractor[ContentKind, Json] = Json.extractor[Int].map {
      case 0 => Mod
      case 1 => Doc
      case _ => Garbage
    }

    implicit val contentKindSerializer: Serializer[ContentKind, Json] =
      Json.serializer[Int].contramap[ContentKind]{
        case Mod => 0
        case Doc => 1
        case Garbage => -1
      }
  }
}
