package mco.ui.desktop.components

import scalafx.Includes._
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.{TableColumn, TableView}
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.cell.ChoiceBoxTableCell
import scalafx.scene.layout.Priority
import scalafx.util.StringConverter

import mco.ui.desktop.UIComponent
import mco.ui.desktop.ObservableBinding._
import mco.{Content, ContentKind}
import mco.ui.state.{UIAction, UpdateContentKind}
import monix.reactive.Observable

object PackageContentTable extends UIComponent[Set[Content], TableView[Content]] {
  override def apply(states: Observable[Set[Content]], act: (UIAction) => Unit): TableView[Content] =
    new TableView[Content] {
      columnResizePolicy = TableView.ConstrainedResizePolicy

      editable = true
      vgrow = Priority.Sometimes
      items =<< states
      columns ++= Seq(
        new TableColumn[Content, String] {
          maxWidth = Double.MaxValue
          text = "Name"
          cellValueFactory = c => ObjectProperty(c.value.key)
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
          cellValueFactory = s => ObjectProperty(s.value.kind)

          onEditCommit = (ev: CellEditEvent[Content, ContentKind]) => {
            act(UpdateContentKind(ev.rowValue.key, ev.newValue))
          }
        }
      )
    }
}
