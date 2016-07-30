package mco.core.general

import mco.core.monad.IO

trait TargetContext {
  def output(data: ContentData): IO[Content]
  def remove(k: String): IO[Unit]
  def flush(): IO[Unit]
}
