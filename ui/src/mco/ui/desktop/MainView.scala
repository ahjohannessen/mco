package mco.ui.desktop


import java.io.{PrintWriter, StringWriter}

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import scalafx.scene.control.cell.{CheckBoxTableCell, ChoiceBoxTableCell, TextFieldTableCell}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import scalafx.util.StringConverter
import scalafx.util.converter.DefaultStringConverter

import cats.data.Xor
import mco.ui.desktop.ObservableFX._
import mco.ui.state._
import mco.{Content, ContentKind, Package}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.PublishSubject

trait MainView {
  def mkStage(initial: Vector[UIState],
              runAction: (UIState, UIAction) => Throwable Xor UIState): JFXApp.PrimaryStage = {
    val initialStates = initial
    val actions = Vector.fill(initialStates.length)(PublishSubject[UIAction]())
    val states = (initialStates zip actions) map { case (state, action) =>
      action
        .scan(state)((state, action) => {
          runAction(state, action).fold(err => {
            new Alert(Alert.AlertType.Error) {
              title = "Error"
              headerText = "Could not complete operation"
              contentText = err.getMessage
              dialogPane().expandableContent = new TextArea {
                text = {
                  val sw = new StringWriter()
                  val pw = new PrintWriter(sw)
                  err.printStackTrace(pw)
                  pw.close()
                  sw.toString
                }
                editable = false
              }
            }.showAndWait()
            state
          }, identity)
        })
        .share
        .startWith(Seq(state))
    }

    new JFXApp.PrimaryStage {
      width = 800
      height = 600
      title = "Mod Collection Organizer"
      scene = new Scene {
        stylesheets += "/mco.ui.desktop/no-focus-outline.css"
        root = new TabPane {
          tabs = (states zip actions) map {
            case (s, a) => uiTab(act => { a.onNext(act); ()})(s)
          }
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
              contentTable(act)(states.map(_.currentPackage).map(_.map(_.contents).getOrElse(Set.empty[Content]))),
              buttons(act)(states.map(_.currentPackage))
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

      editable = true
      items =<< states.map(_.packages)

      hgrow = Priority.Always

      columnResizePolicy = TableView.ConstrainedResizePolicy

      rowFactory = _ => new TableRow[Package] {
        onMouseClicked = handle {
          if (!empty.value) act(SetActivePackage(this.item.value))
        }
      }

      columns ++= Seq(
        new TableColumn[Package, Package] {
          text = "S."
          maxWidth = 32
          resizable = false
          editable = true
          cellFactory = _ => new CheckBoxTableCell[Package, Package](i => {
            def pkg = table.items.getValue.get(i)
            val prop = BooleanProperty(pkg.isInstalled)
            prop.observe()
              .tail
              .foreach(b => act(if (b) InstallPackage(pkg) else UninstallPackage(pkg)))
            prop
          }) {
            padding = Insets(0)
          }
        },
        new TableColumn[Package, String] {
          maxWidth <== table.width - 32
          text = "Package name"
          editable = true
          cellFactory = _ => new TextFieldTableCell[Package, String](new DefaultStringConverter()) {
            editable = true
          }
          cellValueFactory = s => { ObjectProperty(s.value.key) }

          onEditCommit = (ev: CellEditEvent[Package, String]) => {
            // Reset value shown in cell to non-updated,
            // because update might fail later on
            // underlying model, and if it doesn't, we'll
            // redraw it anyway
            table.delegate.refresh()
            act(RenamePackage(ev.oldValue, ev.newValue))
          }
        })
    }

  def buttons: UIComponent[Option[Package], HBox] = act => states =>
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

  def contentTable: UIComponent[Set[Content], TableView[Content]] = act => states => new TableView[Content] { table =>
    columnResizePolicy = TableView.ConstrainedResizePolicy

    editable = true
    vgrow = Priority.Always
    items =<< states
    columns ++= Seq(
      new TableColumn[Content, String] {
        maxWidth = Double.MaxValue
        text = "Name"
        cellValueFactory = { c => ObjectProperty(c.value.key) }
      },
      new TableColumn[Content, ContentKind] {
        minWidth = 75
        text = "Kind"
        cellFactory = _ => new ChoiceBoxTableCell[Content, ContentKind] {
          items ++= Seq(ContentKind.Mod, ContentKind.Doc, ContentKind.Garbage)
          editable = true
          converter = StringConverter(
            ContentKind.fromString _ andThen (_.orNull),
            ContentKind.asString
          )
        }
        cellValueFactory = {s => ObjectProperty(s.value.kind)}

        onEditCommit = (ev: CellEditEvent[Content, ContentKind]) => {
          act(UpdateContentKind(ev.rowValue.key, ev.newValue))
        }
      }
    )
  }
}
