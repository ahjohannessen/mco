package mco.core.general

import scala.language.higherKinds

import better.files.File

trait Target[M[_]] {
  def existing: M[Seq[File]]
  def output(s: String): M[TargetContext]
}