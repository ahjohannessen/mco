package mco

import scala.language.higherKinds

/**
  * Source represents a place where media resides, combined with ability
  * to recreate package information based on existing content only.
  *
  * @tparam M - effect carried by source operations
  */
trait Source[M[_]] {

  /**
    * List all packages and their medium contained that are in this source
    * @return list of (package, media) pairs, such that package.key == media.key
    */
  def list: M[Stream[(Package, Media[M])]]

  /**
    * Add an object described by key (e.g. path or URL) into this source
    * @param key unambigous description of object to be moved in
    * @return fresh (package, media) pair for added object
    */
  def add(key: String): M[(Package, Media[M])]

  /**
    * Remove media described by key
    * @param key key corresponding to media that is to be removed
    * @return
    */
  def remove(key: String): M[Unit]

  /**
    * Rename contained media.
    *
    * This method does not return updated package, but if `list` is used afterwards,
    * the package corresponding to the media will have `key == to`
    * @param from key of media to be renamed
    * @param to new key of this media
    * @return media instance for renamed object
    */
  def rename(from: String, to: String): M[Media[M]]

  /**
    * Check if object described by key can be, in principle, added to the source as
    * a package. Does not need to check for conflicts, but should generally not fail.
    */
  def canAdd(key: String): M[Boolean]
}
