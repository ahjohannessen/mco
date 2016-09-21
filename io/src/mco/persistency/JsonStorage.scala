package mco.persistency

import scala.util.Try

import alleycats.Empty
import mco.io.files.{IO, Path}
import mco.io.files.ops._
import rapture.core.modes.throwExceptions._
import rapture.json._
import rapture.json.jsonBackends.jawn._
import cats.syntax.functor._
import cats.syntax.bifunctor._
import cats.instances.tuple._

class JsonStorage[S: Serializer[?, Json]: Extractor[?, Json]: Empty](target: Path)
  extends (StoreOp[S] => IO[S]) {

  override def apply(v1: StoreOp[S]): IO[S] = v1 match {
    case Update(_, next) => setContent(target, Json(next).toBareString.getBytes("UTF-8")) as next
    case Read => readBytes(target)
      .map(new String(_))
      .map(Json.parse(_))
      .map(json => Try(json.as[S]) getOrElse Empty[S].empty)
    case NoOp(s) => IO.pure(s)
  }

  def applyToLeft[A]: ((StoreOp[S], A)) => (IO[S], A) = _ leftMap this
}
