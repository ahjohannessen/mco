package mco.ui

import scalafx.application.JFXApp

import mco.ui.desktop.MainView

object Runner extends JFXApp with MainView {
  stage = mkStage
}
