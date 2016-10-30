package mco.ui.desktop

import mco.ui.state.UIAction
import monix.reactive.Observable

trait UIComponent[P, U] extends ((Observable[P], UIAction => Unit) => U) {
  def apply(states: Observable[P], act: UIAction => Unit): U
}
