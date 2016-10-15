package mco.ui.desktop


import java.io.{PrintWriter, StringWriter}

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.control._

import cats.data.Xor
import mco.ui.desktop.components.RepoTab
import mco.ui.state._
import monix.execution.Scheduler.Implicits.global
import cats.instances.function._
import cats.syntax.functor._
import monix.reactive.subjects.PublishSubject

trait MainView {
  def mkStage(initial: Vector[UIState],
              runAction: (UIState, UIAction) => Throwable Xor UIState): JFXApp.PrimaryStage = {
    val actions = initial map (_ => PublishSubject[UIAction]())

    val states = for ((action, state) <- actions zip initial)
      yield action
        .scan(state)(doStateTransition(runAction))
        .share
        .startWith(Seq(state))

    val handlers = actions map handlerForSubject[UIAction]

    new JFXApp.PrimaryStage {
      width = 800
      height = 600
      title = "Mod Collection Organizer"
      scene = new Scene {
        stylesheets += "/mco.ui.desktop/no-focus-outline.css"
        root = new TabPane {
          tabs = (states zip handlers) map RepoTab.tupled
        }
      }
    }
  }

  private def handlerForSubject[A](ps: PublishSubject[A]) = (a: A) => { ps.onNext(a); () }

  private def doStateTransition[A, B](run: (A, B) => Throwable Xor A)(a: A, b: B) =
    run(a, b).fold(showExceptionDialog.as(a), identity)

  private val showExceptionDialog = (err: Throwable) => new Alert(Alert.AlertType.Error) {
    title = "Error"
    headerText = "Could not complete operation"
    contentText = err.getMessage
    dialogPane().expandableContent = new TextArea {
      text = {
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        err.printStackTrace(pw)
        pw.close()
        sw.toString
      }
      editable = false
    }
  }.showAndWait()
}
