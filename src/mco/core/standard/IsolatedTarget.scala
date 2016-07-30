package mco.core.standard

import better.files.File
import mco.core.general.{Content, ContentData, Target, TargetContext}
import mco.core.monad.IO

class IsolatedTarget(folder: File) extends Target[IO] {
  override def existing: IO[Seq[File]] = IO { folder.listRecursively.toSeq }

  override def output(s: String): IO[TargetContext] = IO {
    new TargetContext {
      override def flush(): IO[Unit] = IO { }

      override def remove(k: String): IO[Unit] = IO {
        val f = folder / k
        f.delete()
      }

      override def output(data: ContentData): IO[Content] = for {
        _ <- data.writeTo(folder / data.content.key)
      } yield data.content
    }
  }
}
