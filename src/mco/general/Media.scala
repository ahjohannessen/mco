package mco.general

import scala.language.higherKinds

trait Media[M[_]] {
  val key: String
  def readContent: M[Set[String]]
  def copy(content: String)(target: String): M[Unit]
}

object Media {
  trait Companion[M[_]] {
    def apply(entity: String): M[Option[Media[M]]]
  }
}
