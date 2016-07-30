package mco.core.standard

import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.nio.file.StandardOpenOption

import better.files.File
import mco.core.general.Hash
import mco.core.monad.IO
import net.openhft.hashing.LongHashFunction

object Utils {
  private val xxHash = LongHashFunction.xx_r39()
  private val farmHash = LongHashFunction.farmNa()

  def hashFile(file: File): IO[Hash] = mmap(file) { buff =>
    Hash(xxHash hashBytes buff, farmHash hashBytes buff)
  }

  def hashBytes(bytes: Array[Byte]): Hash =
    Hash(xxHash hashBytes bytes, farmHash hashBytes bytes)

  private def mmap[T](file: File)(body: ByteBuffer => T): IO[T] = IO {
    val result = for {
      ch <- file.fileChannel(Seq(StandardOpenOption.READ, StandardOpenOption.WRITE))
      lock = ch.lock()
      buff = ch.map(MapMode.READ_WRITE, 0, file.size)
    } yield body(buff)
    result.head
  }
}