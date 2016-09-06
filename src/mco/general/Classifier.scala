package mco.general

import scala.language.higherKinds

trait Classifier[M[_]] extends (Media[M] => M[Package])
