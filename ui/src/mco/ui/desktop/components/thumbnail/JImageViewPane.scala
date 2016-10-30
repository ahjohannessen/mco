package mco.ui.desktop.components.thumbnail

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.{HPos, VPos}
import javafx.scene.image.{ImageView => JImageView}
import javafx.scene.layout.{Region => JRegion}

private[thumbnail] class JImageViewPane(iv: JImageView) extends JRegion {
  val imageViewProperty = new SimpleObjectProperty[JImageView]()

  imageViewProperty.addListener(new ChangeListener[JImageView]() {
    override def changed(arg0: ObservableValue[_ <: JImageView] , oldIV: JImageView , newIV: JImageView): Unit = {
      if (oldIV != null) {
        getChildren.remove(oldIV)
      }
      if (newIV != null) {
        getChildren.add(newIV)
      }
    }
  })

  imageViewProperty.set(iv)

  def this() = this(new JImageView())

  protected override def layoutChildren(): Unit = {
    val imageView = imageViewProperty.get()
    if (imageView != null) {
      imageView.setFitWidth(getWidth)
      imageView.setFitHeight(getHeight)
      layoutInArea(imageView, 0, 0, getWidth, getHeight, 0, HPos.CENTER, VPos.CENTER)
    }
    super.layoutChildren()
  }
}
