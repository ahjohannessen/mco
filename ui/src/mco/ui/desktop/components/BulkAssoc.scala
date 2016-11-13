package mco.ui.desktop.components

import javafx.scene.image.Image

import scalafx.Includes._
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, TableColumn, TableView}
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.cell.ChoiceBoxTableCell
import scalafx.scene.image.ImageView
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.util.StringConverter

import better.files.File
import cats.implicits._
import mco.ui.desktop.UIComponent
import mco.ui.desktop.ObservableBinding._
import mco.ui.desktop.components.thumbnail.ImageViewPane
import mco.ui.state._
import mco.ui.state.UIState.PendingAdds
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

object BulkAssoc extends UIComponent[PendingAdds, VBox] {
  override def apply(states: Observable[PendingAdds], act: (UIAction) => Unit): VBox = {
    new VBox { contentRoot =>
      hgrow = Priority.Always
      val table = new TableView[(String, Option[String])] { table =>
        editable = true
        columnResizePolicy = TableView.ConstrainedResizePolicy
        items =<< states.map(add => add.packages fproduct add.assoc)

        states
          .distinctUntilChangedByKey(_.packages)
          .foreach(_ => table.delegate.refresh())

        columns ++= Seq(
          new TableColumn[(String, Option[String]), String] {
            text = "Package file"
            cellValueFactory = s => ObjectProperty(lastPathSegment(s.value._1))
          },
          new TableColumn[(String, Option[String]), Option[String]] {
            text = "Package thumbnail"
            private val options = states.map(_.images)
              .map(v => none +: v.map(_.some)).lastSeen()

            cellFactory = _ => {
              val items = options.value getOrElse Vector.empty
              new ChoiceBoxTableCell[(String, Option[String]), Option[String]](
                StringConverter.toStringConverter[Option[String]](o =>
                  o map lastPathSegment getOrElse "<none>"
                ),
                items : _*
              )
            }

            editable = true
            cellValueFactory = s => ObjectProperty(s.value._2)
            onEditCommit = (ev: CellEditEvent[(String, Option[String]), Option[String]]) => {
              table.delegate.refresh()
              act(ReassociatePending(ev.rowValue._1, ev.newValue))
            }
          }
        )
      }

      children = Seq(
        new ImageViewPane(new ImageView {

          image -<< table.selectionModel.observe()
            .flatMap(_.selectedItemProperty.observe())
            .map(Option.apply)
            .map(opt => {
              opt
                .flatMap { case (_, maybeImg) => maybeImg }
                .map(File(_).uri.toString)
                .map(new Image(_))
                .orNull
            })
          preserveRatio = true
          smooth = true
        }) {
          minHeight <== contentRoot.height * 2 / 3
        },
        table,
        new HBox {
          alignment = Pos.CenterRight
          padding = Insets(10, 0, 0, 0)
          spacing = 10
          children = Seq(
            new Button("Cancel") {
              prefWidth = 125
              cancelButton = true
              onAction = handle { act(CancelPendingAdds) }
            },
            new Button("Submit") {
              prefWidth = 125
              defaultButton = true
              onAction -<< states.map(pa => handle { act(SubmitPendingAdds(pa)) })
            }
          )
        }
      )
    }
  }

  private def lastPathSegment(s: String) = {
    val m = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'))
    if (m == -1) s else s.drop(m + 1)
  }
}
