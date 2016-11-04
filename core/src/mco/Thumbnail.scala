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
  def setFrom(location: String): M[Unit]

  /**
    * Remove any thumbnail associated with this media.
    */
  def discard: M[Unit]

  /**
    * Reassociate thumbnails from one media to another
    * Basically a `setThumbnail` on new media followed up with
    * `discardThumbnail` on old one - except no media needed
    *
    * @param to new name of associated package
    */
  def reassociate(to: String): M[Unit]


  final def mapK[G[_]](f: M ~> G): Thumbnail[G] = new Thumbnail[G] {
    override def url: G[Option[URL]] = f(self.url)
    override def discard: G[Unit] = f(self.discard)
    override def setFrom(location: String): G[Unit] = f(self.setFrom(location))
    override def reassociate(to: String): G[Unit] = f(self.reassociate(to))
  }
}
