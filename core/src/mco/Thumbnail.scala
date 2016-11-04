package mco

import java.net.URL

import scala.language.higherKinds

import cats.~>

trait Thumbnail[M[_]] { self =>
  /**
    * Get thumbnail associated with this media's package.
    * Thumbnail is provided as [[java.net.URL]] for convenience
    * @return `Some(< thumbnail URL >)`, if thumbnail exists
    */
  def url: M[Option[URL]]

  /**
    * Associate thumbnail from given location with this media.
    * @param location place to look thumbnail for
    */
  def setThumbnail(location: String): M[Unit]

  /**
    * Remove any thumbnail associated with this media.
    */
  def discardThumbnail: M[Unit]

  /**
    * Reassociate thumbnails from one media to another
    * Basically a `setThumbnail` on new media followed up with
    * `discardThumbnail` on old one - except no media needed
    *
    * @param from old name of associated package
    * @param to new name of associated package
    */
  def reassociate(from: String, to: String): M[Unit]


  final def mapK[G[_]](f: M ~> G): Thumbnail[G] = new Thumbnail[G] {
    override def url: G[Option[URL]] = f(self.url)
    override def discardThumbnail: G[Unit] = f(self.discardThumbnail)
    override def setThumbnail(location: String): G[Unit] = f(self.setThumbnail(location))
    override def reassociate(from: String, to: String): G[Unit] = f(self.reassociate(from, to))
  }
}
