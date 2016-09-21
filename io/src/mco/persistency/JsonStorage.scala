package mco.persistency

import scala.util.Try

import alleycats.Empty
import cats.instances.tuple._
import cats.syntax.bifunctor._
import cats.syntax.functor._
import mco.ContentKind
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
  import ContentKind._
  implicit val contentKindExtractor: Extractor[ContentKind, Json] = Json.extractor[Int].map{
    case 0 => Mod
    case 1 => Doc
    case _ => Garbage
  }

  implicit val contentKindSerializer: Serializer[ContentKind, Json] =
    Json.serializer[Int]
      .contramap[ContentKind]{
      case Mod => 0
      case Doc => 1
      case Garbage => -1
    }
}