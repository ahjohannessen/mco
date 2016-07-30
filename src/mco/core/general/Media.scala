package mco.core.general

import better.files.File
import mco.core.monad.IO


object Media {
  trait Companion {
    def apply(file: File): IO[Option[Media]]
  }
}

trait Media {
  def readContent: IO[Set[Content]]
  def readData(c: Content): IO[ContentData]
  final def hashMedia(cnt: Set[Content]): Hash =
    cnt
    .iterator
    .map {_.hash}
    .fold(Hash(0L, 0L)){ case (Hash(a1, a2), Hash(b1, b2)) => Hash(a1 ^ b1, a2 ^ b2) }
}