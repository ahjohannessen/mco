package mco.general
package stubs

import better.files.File
import mco.core.monad.IO
import mco.io.DiskIO

class NoopContentData(cnt: Content) extends ContentData {
  override val content: Content = cnt
  override def writeTo(p: File): DiskIO[File] = IO { p }
}
