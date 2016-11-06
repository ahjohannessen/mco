package mco.ui.desktop.components.thumbnail

import java.net.URL
import javafx.scene.image.Image

import scalafx.scene.image.ImageView
import scalafx.scene.layout.Priority

import mco.ui.desktop.{DropFilesReceiver, UIComponent}
import mco.ui.state.{AddObjects, AdditionContext, UIAction}
import mco.ui.desktop.ObservableBinding._
import monix.reactive.Observable

object PackageThumbnail extends UIComponent[(Option[URL], Double), ImageViewPane] {
  override def apply(states: Observable[(Option[URL], Double)], act: UIAction => Unit): ImageViewPane =
    new ImageViewPane(new ImageView {
      image -<< states.map(_._1).map(_.map(url => new Image(url.toString)).orNull)
      preserveRatio = true
      smooth = true
    }) with DropFilesReceiver {

      minHeight -<< states.map(_._2)
      hgrow = Priority.Always
      vgrow = Priority.Always

      override def onFilesReceived(paths: Vector[String]): Unit = {
        act(AddObjects(paths, AdditionContext.Thumbnail))
      }
    }
}
