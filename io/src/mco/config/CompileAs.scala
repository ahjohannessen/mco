package mco.config

import scala.reflect.runtime._
import scala.reflect.runtime.universe.TypeTag
import scala.tools.reflect.ToolBox
import scala.util.Try

object CompileAs {
  def apply[A: TypeTag](fullName: String): Try[A] = {
    val toolbox = currentMirror.mkToolBox()
    Try {
      require(fullName matches "^[0-9a-zA-Z\\.]*$", s"Invalid name: $fullName")
      s"$fullName: ${implicitly[TypeTag[A]].tpe}"
    } map toolbox.parse map toolbox.compile map (eval => eval().asInstanceOf[A])
  }
}
