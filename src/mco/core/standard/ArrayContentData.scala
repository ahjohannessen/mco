package mco.core.standard

import better.files.File
import mco.core.general.{Content, ContentData}
import mco.core.monad.IO

class ArrayContentData(val content: Content, bytes: Array[Byte]) extends ContentData {
  override def writeTo(p: File): IO[File] = IO { p.write(bytes)(File.OpenOptions.default) }
}
