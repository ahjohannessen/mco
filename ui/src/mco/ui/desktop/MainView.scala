package mco.ui.desktop


import java.io.{PrintWriter, StringWriter}

import scala.language.higherKinds
import scala.util.{Failure, Success, Try}
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.control._

import cats.instances.vector._
import cats.syntax.functor._
import mco.ui.desktop.components.RepoTab
import mco.ui.state._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.PublishSubject

trait MainView {
  def mkStage[F[_], A](exec: ExecState[F, A]): JFXApp.PrimaryStage = {
    val loadResult = for {
      initialModels <- exec.attemptRun(exec.initial)
      actions = initialModels map (_ => PublishSubject[UIAction]())
      handlers = actions map handlerForSubject[UIAction]
      initialStates = initialModels.fproduct(exec.loadInitialState)
      states = for ((action, state) <- actions zip initialStates) yield {
        val runResults = action
          .scan(state)(attemptReaction({exec.react _}.tupled andThen exec.attemptRun))
          .share
          .startWith(Seq(state))

        runResults map { case (_, uiState) => uiState }
      }
    } yield states zip handlers

    loadResult.map { objects =>
      new JFXApp.PrimaryStage {
        width = 800
        height = 600
        title = "Mod Collection Organizer"
        scene = new Scene {
          stylesheets += "/mco.ui.desktop/no-focus-outline.css"
          root = new TabPane { tabs = objects map RepoTab.tupled }
        }
      }
    }.get
  }

  private def handlerForSubject[A](ps: PublishSubject[A]) = (a: A) => { ps.onNext(a); () }

  private def attemptReaction[A, B](run: ((A, B)) => Try[A])(a: A, b: B) =
    run((a, b)) match {
      case Success(r) => r
      case Failure(ex) =>
        showExceptionDialog(ex)
        a
    }

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
