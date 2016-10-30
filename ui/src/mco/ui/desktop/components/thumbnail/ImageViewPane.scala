package mco.ui.desktop.components.thumbnail

import scalafx.delegate.SFXDelegate
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Region

class ImageViewPane private[thumbnail](override val delegate: JImageViewPane)
  extends Region(delegate) with SFXDelegate[JImageViewPane]
{
  def this(iv: ImageView) = this(new JImageViewPane(iv.delegate))
}
