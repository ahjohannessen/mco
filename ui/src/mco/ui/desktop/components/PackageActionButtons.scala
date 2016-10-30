package mco.ui.desktop.components

import scalafx.Includes._
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Button
import scalafx.scene.layout.{HBox, Priority, Region}

import mco.{ContentKind, Package}
import mco.ui.desktop.UIComponent
import mco.ui.desktop.ObservableBinding._
import mco.ui.state.{InstallPackage, UIAction, UninstallPackage}
import monix.reactive.Observable

object PackageActionButtons extends UIComponent[Option[Package], HBox] {
  override def apply(states: Observable[Option[Package]], act: (UIAction) => Unit): HBox =
    new HBox {
      hgrow = Priority.Always
      alignmentInParent = Pos.CenterRight
      padding = Insets(10, 0, 10, 0)
      spacing = 10
      children = Seq(
        new Region {
          minWidth = 10
          maxWidth = Double.MaxValue
          hgrow = Priority.Always
        },
        new Button("View README") {
          prefWidth = 125

          disable -<< states
            .map(!_.exists(_.contents.exists(_.kind == ContentKind.Doc)))

          onMouseClicked = handle { sys.error("Readme not implemented") }
        },
        new Button {
          prefWidth = 125
          text -<< states
            .map(_.forall(!_.isInstalled))
            .map(if (_) "Install" else "Uninstall")

          disable -<< states.map(_.isEmpty)

          onAction -<< states.collect { case Some(x) => handle {
            act(if (!x.isInstalled) InstallPackage(x) else UninstallPackage(x))
          }}
          defaultButton = true
        }
      )
    }
}
