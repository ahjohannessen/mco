package mco.core.general
package stubs

import mco.core.monad.IO

class TracingTargetContext extends TargetContext {
  import TracingTargetContext._
  var trace = Vector.empty[Trace]

  override def output(data: ContentData): IO[Content] = IO {
    trace :+= Output(data.content)
    data.content
  }

  override def remove(k: String): IO[Unit] = IO { trace :+= Remove(k) }

  override def flush(): IO[Unit] = IO { trace :+= Close }
}

object TracingTargetContext {
  sealed trait Trace
  case class Remove(k: String) extends Trace
  case class Output(c: Content) extends Trace
  case object Close extends Trace
}