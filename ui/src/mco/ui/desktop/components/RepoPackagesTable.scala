package mco.ui.desktop.components

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.geometry.Insets
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{TableColumn, TableRow, TableView}
import scalafx.scene.control.cell.{CheckBoxTableCell, TextFieldTableCell}
import scalafx.scene.layout.Priority
import scalafx.util.converter.DefaultStringConverter

import mco.Package
import mco.ui.desktop.UIComponent
import mco.ui.desktop.ObservableBinding._
import mco.ui.state._
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

object RepoPackagesTable extends UIComponent[UIState, TableView[Package]] {
  override def apply(states: Observable[UIState], act: (UIAction) => Unit): TableView[Package] =
    new TableView[Package] { table =>
      states
        .map(s => s.currentPackage.map(s.packages.indexOf))
        .foreach(_.foreach(i => Platform.runLater {
          // TODO - this does not work in all cases
          table.selectionModel.value.clearAndSelect(i)
          table.focusModel.value.focus(i)
        }))

      editable = true
      items =<< states.map(_.packages)

      hgrow = Priority.Always

      columnResizePolicy = TableView.ConstrainedResizePolicy

      rowFactory = _ => new TableRow[Package] {
        onMouseClicked = handle {
          if (!empty.value) act(SetActivePackage(item.value))
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
}
