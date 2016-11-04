package mco

import scala.language.higherKinds

/**
  * Media represents a storage associated with [[mco.Package]]
  * @tparam M - effect of media operations
  */
trait Media[M[_]] {
  /**
    * A key uniquely identifying this media in parent Source
    */
  val key: String

  /**
    * Keys identifying underlying storage contents
    * @return Set of keys for what this media contains
    */
  def contentKeys: M[Set[String]]

  /**
    * Copy content according to provided Map
    * Map keys should be a non-strict subset of #contentKeys
    * Meaning of values is implementation defiled
    *
    * @param locations mapping from content keys to locations
    */
  def copy(locations: Map[String, String]): M[Unit]

  /**
    * get thumbnail associated with current Media
    * @return
    */
  def thumbnail: Thumbnail[M]
}

object Media {

  /**
    * Type for [[mco.Media]] factories
    * @tparam M - effect of media operations
    */
  trait Companion[M[_]] extends (String => M[Option[Media[M]]])
}
