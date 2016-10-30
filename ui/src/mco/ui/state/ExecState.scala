package mco.ui.state

import scala.language.higherKinds
import scala.util.Try


trait ExecState[F[_], R] {
  def attemptRun[A](a: F[A]): Try[A]
  def initial: F[Vector[R]]
  def loadInitialState(r: R): UIState
  def react(prev: (R, UIState), action: UIAction): F[(R, UIState)]
}
