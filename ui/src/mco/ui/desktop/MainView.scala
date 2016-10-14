package mco.ui.desktop

import java.util.Base64

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.scene.layout.{HBox, Priority, Region, VBox}

import mco.ui.desktop.ObservableFX._
import mco.ui.state._
import mco.{Content, ContentKind, Package}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.PublishSubject

trait MainView {
  def mkStage: JFXApp.PrimaryStage = {
    val initialStates = initial
    val actions = Vector.fill(initialStates.length)(PublishSubject[UIAction]())
    val states = (initialStates zip actions) map {
      case (state, action) => action.scan(state)(runAction).share.startWith(Seq(state))
    }

    new JFXApp.PrimaryStage {
      width = 800
      height = 600
      title = "Mod Collection Organizer"
      scene = new Scene {
        stylesheets += "/mco.ui.desktop/no-focus-outline.css"
        root = new TabPane {
          tabs = (states zip actions).map{case (states, actions) => uiTab(act => { actions.onNext(act); ()})(states)}
        }
      }
    }
  }

  def uiTab: UIComponent[UIState, Tab] = act => states =>
    new Tab {
      text -<< states.map(_.repoName)
      closable = false
      content = new HBox { contentRoot =>
        padding = Insets(10)
        children = Seq(
          packageListing(act)(states),
          new VBox {
            padding = Insets(0, 0, 0, 10)
            prefWidth <== (contentRoot.width / 3)
            children = Seq(
              new Label("Images not yet supported") {
                vgrow = Priority.Always
              },
              new TableView[Content] {
                vgrow = Priority.Always
                items =<< states.map(_.currentPackage).map(_.map(_.contents).getOrElse(Set.empty[Content]))
                columns ++= Seq(
                  new TableColumn[Content, String] {
                    text = "Name"
                    cellValueFactory = { c => ObjectProperty(c.value.key) }
                  }
                )
              },
              new HBox {
                hgrow = Priority.Always
                alignmentInParent = Pos.CenterRight
                padding = Insets(10, 0, 10, 0)
                spacing = 10
                fillWidth = true
                children = Seq(
                  new Region {
                    minWidth = 10
                    maxWidth = Double.MaxValue
                    hgrow = Priority.Always
                  },
                  new Button("View README") {
                    prefWidth = 125
                    disable -<< states.map(!_.currentPackage.exists(_.contents.exists(_.kind == ContentKind.Doc)))
                    onMouseClicked = handle { sys.error("Readme not implemented") }
                  },
                  new Button {
                    prefWidth = 125
                    private val current = states.map(_.currentPackage)
                    text -<< current
                      .map(_.forall(!_.isInstalled))
                      .map(if (_) "Install" else "Uninstall")

                    disable -<< current.map(_.isEmpty)

                    onMouseClicked -<< current.collect{ case Some(x) => handle {
                      act(if (!x.isInstalled) InstallPackage(x) else UninstallPackage(x))
                    }}


                    defaultButton = true
                  }
                )
              }
            )
          }
        )
      }
    }

  def packageListing: UIComponent[UIState, TableView[Package]] = act => states =>
    new TableView[Package] { table =>
      states
        .map(s => s.currentPackage.map(s.packages.indexOf))
        .foreach(_.foreach(i => Platform.runLater {
          table.selectionModel.value.clearAndSelect(i)
          table.focusModel.value.focus(i)
        }))
      items =<< states.map(_.packages)

      hgrow = Priority.Always

      columnResizePolicy = TableView.ConstrainedResizePolicy

      rowFactory = _ => new TableRow[Package] {
        onMouseClicked = handle {
          if (!this.empty.value) act(SetActivePackage(this.item.value))
        }
      }

      columns ++= Seq(
        new TableColumn[Package, Package] {
          text = "S."
          maxWidth = 32
          resizable = false
          cellFactory = column => new CheckBoxTableCell[Package, Package](i => BooleanProperty(table.items.getValue.get(i).isInstalled)) {
            padding = Insets(0)
            onMouseClicked = handle {
              val x = item.value
              act(if (!x.isInstalled) InstallPackage(x) else UninstallPackage(x))
            }
          }
          cellValueFactory = { s => ObjectProperty(s.value) }

        },
        new TableColumn[Package, String] {
          maxWidth <== table.width - 32
          text = "Package name"
          cellValueFactory = { s => ObjectProperty(s.value.key) }
        })
    }
}
