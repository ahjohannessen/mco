package mco.ui.desktop

import javafx.beans.property.ObjectProperty
import javafx.collections.ObservableList

import scalafx.application.Platform.runLater
import scalafx.beans.property.Property
import scalafx.beans.value.ObservableValue
import scalafx.collections.{fillCollection, fillSFXCollection}
import scalafx.delegate.SFXDelegate

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.subjects.BehaviorSubject

object ObservableBinding {
  implicit class PropertyOps[T, J](val self: Property[T, J]) extends AnyVal {
    def -<<(obs: Observable[T]): Unit = { obs.foreach(x => runLater(self.update(x))); () }
    def =<<[B](obs: Observable[_ <: Iterable[B]])(implicit ev: T <:< ObservableList[B]): Unit = {
      obs.foreach(list => runLater(fillCollection(self.value, list))); ()
    }
  }

  implicit class ObservableValueOps[T, J](val self: ObservableValue[T, J]) {
    def observe(): Observable[T] = {
      val subj = BehaviorSubject(self.value)
      self.onChange {
        subj.onNext(self.value)
        ()
      }
      subj.onNext(self.value)
      subj
    }
  }

  implicit class JObjectPropertyOps[J <: AnyRef](val self: ObjectProperty[J]) extends AnyVal {
    def -<<(obs: Observable[J]): Unit = { obs.foreach(x => runLater(self.set(x))); () }
    def =<<[B](obs: Observable[_ <: Iterable[B]])(implicit ev: J <:< ObservableList[B]): Unit = {
      obs.foreach(list => runLater(fillCollection(self.get(), list))); ()
    }
  }

  implicit class ObservableListOps[A <: AnyRef](val self: ObservableList[A]) extends AnyVal {
    def =<<(obs: Observable[_ <: Iterable[SFXDelegate[A]]]): Unit = {
      obs.foreach(list => runLater(fillSFXCollection(self, list))); ()
    }

    def -<<(obs: Observable[_ <: SFXDelegate[A]]): Unit = self =<< obs.map(Seq(_))
  }
}
