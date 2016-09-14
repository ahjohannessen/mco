package mco.utils

import cats.syntax.option._

trait WhenOperator {
  @inline def when[A](cond: => Boolean)(body: => A): Option[A] = if (cond) body.some else none
}

object WhenOperator extends WhenOperator