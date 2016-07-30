package mco.core.general

import better.files.File
import mco.core.monad.IO

trait ContentData {
  val content: Content
  def writeTo(p: File): IO[File]
}
