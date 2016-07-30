package mco.core.general

import scala.language.higherKinds

import better.files.File

trait Source[M[_]] {
  def add(f: File): M[Source[M]]
  def remove(s: String): M[Source[M]]
  def list: M[Map[String, Media]]
}