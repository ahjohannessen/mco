package mco

import scala.language.higherKinds

import cats.Functor
import cats.syntax.functor._

sealed trait Classifier[M[_]] extends (Media[M] => M[Package])

object Classifier {
//  def monadic[M[_]: Monad](fn: String => M[ContentKind]): Classifier[M] = new Classifier[M] {
//    override def apply(v1: Media[M]): M[Package] =
//      for {
//        keys <- v1.readContent
//        contents <- keys traverseU {key => fn(key) map (kind => Content(key, kind))}
//      } yield Package(v1.key, contents)
//  }
//
//  def arbitrary[M[_]](fn: Media[M] => M[Package]): Classifier[M] = new Classifier[M] {
//    override def apply(v1: Media[M]): M[Package] = fn(v1)
//  }

  def apply[M[_]: Functor](fn: String => ContentKind): Classifier[M] = new Classifier[M] {
    override def apply(v1: Media[M]): M[Package] = for {
      keys <- v1.readContent
    } yield Package(v1.key, keys map { key => Content(key, fn(key)) })
  }
}
