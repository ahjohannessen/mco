package mco

import java.net.URL

import scala.language.higherKinds

trait Media[M[_]] {
  val key: String
  def contentKeys: M[Set[String]]
  def copy(content: Map[String, String]): M[Unit]
  def thumbnail: M[Option[URL]]
}

object Media {
  trait Companion[M[_]] extends (String => M[Option[Media[M]]])
}
