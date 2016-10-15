package mco.ui

import scalafx.application.JFXApp

import mco.ui.desktop.MainView
import mco.ui.state.TestState

object Runner extends JFXApp with MainView {
  stage = mkStage(TestState.initial, TestState.runAction)
}
