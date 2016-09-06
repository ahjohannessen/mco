package mco.general

import scala.language.higherKinds

import cats.data.Xor

trait Source[M[_]] {
  def list: M[Stream[(Package, Media[M])]]

  def add(f: String): M[String Xor Source[M]]
  def remove(s: String): M[String Xor Source[M]]
  def rename(from: String, to: String): M[Source[M]]
}