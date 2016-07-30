package mco.core.general
package stubs

import mco.core.monad.IO

class NoopMedia(contents: Content*) extends Media {
  override def readContent: IO[Set[Content]] = IO { contents.toSet }
  override def readData(c: Content): IO[ContentData] = IO { new NoopContentData(c) }
}

