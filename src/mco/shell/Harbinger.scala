package mco.shell

import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try

import mco.core.general.{Media, Package}

// TODO temporary name
object Harbinger {
  private def loadCompanion[T: ClassTag](s: String): Try[T] = {
    val rtc = implicitly[ClassTag[T]].runtimeClass
    def name = rtc.getDeclaringClass.getCanonicalName
    Try {
      val cl = Class.forName(s + "$")
      val obj = cl.getField("MODULE$").get(cl)
      rtc.cast(obj).asInstanceOf[T]
    } recover {
      case ex @ (_: ClassCastException | _: ClassNotFoundException) =>
        throw new ClassNotFoundException(
          s"$s must be a $name and have a companion object extending Companion trait"
        )
    }
  }

  def loadMedia(s: String) = loadCompanion[Media.Companion](s)
//  def loadRepo[M[_]](s: String) = loadCompanion[Repo.Companion[M]](s)
  def loadPackage(s: String) = loadCompanion[Package.Companion](s)
}
