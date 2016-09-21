package mco

sealed trait Fail

object Fail {
  case class Uncaught(ex: Throwable) extends Fail
}
