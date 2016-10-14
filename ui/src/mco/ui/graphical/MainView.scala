package mco.ui.graphical

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{HBox, VBox}

import cats.instances.all._
import cats.syntax.all._
import mco.{Content, ContentKind, Package}
import mco.ui.graphical.ObservableFX._
import mco.ui.state._
import monix.reactive.subjects.{PublishSubject, ReplaySubject}
import monix.execution.Scheduler.Implicits.global

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
      content = new HBox {
        children = Seq(
          packageListing(act)(states map (_.packages)),
          new VBox {
            children = Seq(
              new Label("Images not yet supported"),
              new TableView[Content] {
                items =<< states.map(_.currentPackage).map(_.map(_.contents).getOrElse(Set.empty[Content]))
                columns ++= Seq(
                  new TableColumn[Content, String] {
                    text = "Name"
                    cellValueFactory = { c => ObjectProperty(c.value.key) }
                  }
                )
              },
              new HBox {
                alignmentInParent = Pos.CenterLeft
                padding = Insets(10)
                spacing = 10
                fillWidth = true
                children = Seq(
                  new Button("View README") {
                    disable -<< states.map(!_.currentPackage.exists(_.contents.exists(_.kind == ContentKind.Doc)))
                    onMouseClicked = handle { sys.error("Readme not implemented") }
                  },
                  new Button {
                    private val current = states.map(_.currentPackage)
                    text -<< current
                      .map(_.forall(!_.isInstalled))
                      .map(if (_) "Install" else "Uninstall")

                    disable -<< current.map(_.isEmpty)

                    onMouseClicked -<< current.collect{ case Some(x) => handle {
                      act(if (!x.isInstalled) InstallPackage(x) else UninstallPackage(x))
                    }}


                    defaultButton = true
                    style = "-fx-font-weight: bold"
                  }
                )
              }
            )
          }
        )
      }
    }

  def packageListing: UIComponent[Seq[Package], TableView[Package]] = act => states =>
    new TableView[Package] { table =>
      items =<< states

      rowFactory = _ => new TableRow[Package] {
        onMouseClicked = handle {
          if (!this.empty.value) act(SetActivePackage(this.item.value))
        }
      }

      columns ++= Seq(
        new TableColumn[Package, Package] {
          text = "S."
          cellFactory = column => new CheckBoxTableCell[Package, Package](i => BooleanProperty(table.items.getValue.get(i).isInstalled)) {
            onMouseClicked = handle {
              val x = item.value
              act(if (!x.isInstalled) InstallPackage(x) else UninstallPackage(x))
            }
          }
          cellValueFactory = { s => ObjectProperty(s.value) }

        },
        new TableColumn[Package, String] {
          text = "Package name"
          cellValueFactory = { s => ObjectProperty(s.value.key) }
        })
    }
}
