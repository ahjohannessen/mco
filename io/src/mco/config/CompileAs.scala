package mco.config

import scala.reflect.runtime._
import scala.reflect.runtime.universe.TypeTag
import scala.tools.reflect.ToolBox
import scala.util.Try
import scala.util.control.NonFatal

import mco.Fail

object CompileAs {
  def apply[A: TypeTag](fullName: String): Try[A] = {
    val toolbox = currentMirror.mkToolBox()
    Try {
      require(fullName matches "^[0-9a-zA-Z\\.]*$", s"Invalid name: $fullName")
      s"$fullName: ${implicitly[TypeTag[A]].tpe}"
    } map toolbox.parse map toolbox.compile recover {
      case NonFatal(_) => throw Fail.MissingResource(fullName)
    } map (eval => eval().asInstanceOf[A])
  }
}
