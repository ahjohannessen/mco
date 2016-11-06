package mco.ui.state

sealed trait AdditionContext

object AdditionContext {
  case object Packages extends AdditionContext
  case object Thumbnail extends AdditionContext
}
