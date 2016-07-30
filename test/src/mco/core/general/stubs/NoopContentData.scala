package mco.core.general
package stubs

import better.files.File
import mco.core.monad.IO

class NoopContentData(cnt: Content) extends ContentData {
  override val content: Content = cnt
  override def writeTo(p: File): IO[File] = IO { p }
}
