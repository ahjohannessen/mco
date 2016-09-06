package mco.general
package stubs

import mco.core.monad.IO
import mco.io.DiskIO

class NoopMedia(contents: Content*) extends Media {
  override def readContent: DiskIO[Set[Content]] = IO { contents.toSet }
  override def readData(c: Content): DiskIO[ContentData] = IO { new NoopContentData(c) }
}

