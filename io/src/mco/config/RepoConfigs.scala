package mco.config

import scala.util.Try

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.EnumerationReader._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object RepoConfigs {
  def apply(c: Config): Try[Seq[RepoConfig]] = Try { c.as[Seq[RepoConfig]]("mco.io.repositories") }
}
