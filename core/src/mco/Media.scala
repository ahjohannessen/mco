package mco

import scala.language.higherKinds

trait Media[M[_]] {
  val key: String
  def readContent: M[Set[String]]
  def copy(content: Map[String, String]): M[Unit]
}

object Media {
  trait Companion[M[_]] extends (String => M[Option[Media[M]]])
}
