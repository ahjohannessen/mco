package mco

import scala.language.higherKinds

trait Source[M[_]] {
  def list: M[Stream[(Package, Media[M])]]

  def add(f: String): M[Source[M]]
  def remove(s: String): M[Source[M]]
  def rename(from: String, to: String): M[(Source[M], Media[M])]
}