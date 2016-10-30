package mco.ui.state

object PipeSyntax {
  implicit class Piped[A](val self: A) {
    def |>[B](f: A => B): B = f(self)
  }
}
