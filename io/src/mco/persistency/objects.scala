package mco.persistency

//import simulacrum.typeclass
//import scala.language.implicitConversions
//
//sealed trait Delta[A]
//case class Add[A] (a: A) extends Delta[A]
//case class Remove[A] (a: A) extends Delta[A]

sealed trait StoreOp[+A]
case class Update[A] (old: A, next: A) extends StoreOp[A]
case object Read extends StoreOp[Nothing]

//// $COVERAGE-OFF$Macro-generated code
//@typeclass trait Separatable[A] {
//  def delta(old: A, next: A): Delta[A]
//}
//// $COVERAGE-ON