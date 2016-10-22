package mco.ui

import scalafx.application.JFXApp

import mco.ui.desktop.MainView
import mco.ui.state.ExecTestState

object Runner extends JFXApp with MainView {
  stage = mkStage(ExecTestState)
}
