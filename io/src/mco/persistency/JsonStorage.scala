package mco.persistency

import scala.util.Try

import alleycats.Empty
import cats.instances.tuple._
import cats.syntax.applicative._
import cats.syntax.bifunctor._
import cats.syntax.functor._
import cats.~>
import mco.{EffectRepo, _}
import mco.io.files._
import rapture.core.modes.throwExceptions._
import rapture.json._
import rapture.json.jsonBackends.jawn._

class JsonStorage[S: Serializer[?, Json]: Extractor[?, Json]: Empty](target: Path)
  extends (StoreOp[S] => IO[S]) {

  override def apply(v1: StoreOp[S]): IO[S] = v1 match {
    case Update(_, next) => setContent(target, Json(next).toBareString.getBytes("UTF-8")) as next
    case Read => for {
      isFile <- isRegularFile(target)
      data <- if (isFile) readBytes(target).map(new String(_))
              else IO.pure("")
    } yield Try(Json.parse(data).as[S]) getOrElse Empty[S].empty
    case NoOp => Empty[S].empty.pure[IO]
  }

  def applyToLeft[A]: ((StoreOp[S], A)) => (IO[S], A) = _ leftMap this
}

object JsonStorage {
  def preload[S: Serializer[?, Json]: Extractor[?, Json]: Empty]
  (
    key: String,
    repo: Repository.Companion[IO, S],
    json: Path,
    target: String,
    source: Source[IO]
  ): IO[Repository[IO, Unit]] = {
    for {
      storage <- new JsonStorage[S](json).pure[IO]
      state <- storage(Read)
      loaded <- repo(key, source, target, state)
      pr = new PersistedRepo(loaded).widen
    } yield new EffectRepo[PersistedRepo.Lift[IO, S]#T, IO, Unit](pr, outputState(storage)).widen
  }

  private def outputState[S](st: JsonStorage[S]) = new (PersistedRepo.Lift[IO, S]#T ~> IO) {
    override def apply[A](fa: IO[(StoreOp[S], A)]): IO[A]
      = fa map st.applyToLeft[A] flatMap { case (io, a) => io as a }
  }

  object Converters {
    import ContentKind._
    implicit val contentKindExtractor: Extractor[ContentKind, Json] =
      Json.extractor[String] map {ContentKind.fromString} map {_ getOrElse Garbage}

    implicit val contentKindSerializer: Serializer[ContentKind, Json] =
      Json.serializer[String].contramap[ContentKind]{ContentKind.asString}
  }
}
