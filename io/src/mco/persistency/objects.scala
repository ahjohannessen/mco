package mco.persistency

sealed trait StoreOp[+A]
case class Update[A] (old: A, next: A) extends StoreOp[A]
case object Read extends StoreOp[Nothing]
case object NoOp extends StoreOp[Nothing]
