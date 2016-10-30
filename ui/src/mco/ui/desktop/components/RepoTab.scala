package mco.ui.desktop.components

import scalafx.geometry.Insets
import scalafx.scene.control.{Label, Tab}
import scalafx.scene.layout.{HBox, Priority, VBox}

import mco.Content
import mco.ui.desktop.UIComponent
import mco.ui.desktop.ObservableBinding._
import mco.ui.state.{UIAction, UIState}
import monix.reactive.Observable

object RepoTab extends UIComponent[UIState, Tab] {
  override def apply(states: Observable[UIState], act: (UIAction) => Unit): Tab = new Tab {
    text -<< states.map(_.repoName)
    closable = false
    content = new HBox { contentRoot =>
      padding = Insets(10)
      children = Seq(
        RepoPackagesTable(states, act),
        new VBox {
          padding = Insets(0, 0, 0, 10)
          prefWidth <== (contentRoot.width / 3)
          children = Seq(
            new Label("Images not yet supported") {
              vgrow = Priority.Always
            },
            PackageContentTable(states.map(_.currentPackage).map(_.map(_.contents).getOrElse(Set.empty[Content])), act),
            PackageActionButtons(states.map(_.currentPackage), act)
          )
        }
      )
    }
  }
}
