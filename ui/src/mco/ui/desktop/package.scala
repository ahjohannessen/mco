package mco.ui

import mco.ui.state.UIAction
import monix.reactive.Observable
import monix.reactive.subjects.Subject

package object desktop {
  type UIComponent[S, U] = (UIAction => Unit) => Observable[S] => U
}
