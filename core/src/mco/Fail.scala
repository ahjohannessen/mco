package mco

sealed trait Fail extends Throwable {
  def rethrow(): Nothing = throw this
}

object Fail {
  case class Uncaught(ex: Throwable)
    extends RuntimeException(ex) with Fail

  case class MissingResource(name: String)
    extends RuntimeException(s"Resource $name is missing") with Fail

  case class NameConflict(name: String)
    extends RuntimeException(s"Name conflict: $name") with Fail

  case class UnexpectedType(name: String, expected: String)
    extends RuntimeException(s"Expected $name to be $expected") with Fail
}
