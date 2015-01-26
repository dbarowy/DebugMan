package edu.umass.cs.plasma.automandebugger

import java.util.{UUID, Date}
import akka.actor.Actor
import edu.umass.cs.automan.core.info.StateInfo
import edu.umass.cs.automan.core.info.QuestionInfo
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import spray.routing._
import edu.umass.cs.automan.core.AutomanAdapter
import spray.json._
import edu.umass.cs.plasma.automandebugger.DebugJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class DebugServer[T <: AutomanAdapter](a: T) extends Actor with DebugService[T] {
  val adapter = a

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(routes)
}

// this trait defines our service behavior independently from the service actor
trait DebugService[T <: AutomanAdapter] extends HttpService {
  val adapter: T

  val routes =
    path("data") {
      get {
        complete {
          debug_info.toJson.toString()
        }
      }
    } ~
    path("") {
      getFromResource("automan-plugins/debugger/index.html")
    } ~
    path("debugger") {
      getFromResource("automan-plugins/debugger/index.html")
    } ~
    path("task") {
      getFromResource("automan-plugins/debugger/DebuggerConsole.html")
    } ~
    path("testdata") {
      getFromResource("automan-plugins/debugger/testdata/AutoManDebuggerJSON.json")
    } ~
    pathPrefix("js") {
      getFromResourceDirectory("automan-plugins/debugger/js")
    } ~
    pathPrefix("css") {
      getFromResourceDirectory("automan-plugins/debugger/css")
    }

  private def average_thunk_time(thunks: List[Thunk[_]]) : Double = {
    val ans_ts = answered_thunks(thunks)
    if (ans_ts.size == 0) {
      Double.NaN
    } else {
      ans_ts.foldLeft(0L)((acc,t) =>
        acc + (Utilities.dateToTimestamp(t.completed_at.get) - Utilities.dateToTimestamp(t.created_at))
      ) / ans_ts.size.toDouble
    }
  }

  private def num_answers_received(thunks: List[Thunk[_]]) : Int = {
    answered_thunks(thunks).size
  }

  private def answered_thunks(thunks: List[Thunk[_]]) : List[Thunk[_]] = {
    thunks.filter(_.answer != None)
  }

  private def average_payout(dont_reject: Boolean, amount_paid: BigDecimal, thunks: List[Thunk[_]]) : BigDecimal = {
    // count rejections toward paid thunk count when dont_reject == true
    // we do not count PROCESSED thunks toward average
    // since they were paid for in a previous execution
    val is_paid = (t: Thunk[_]) => if (dont_reject) {
      t.state == SchedulerState.ACCEPTED ||
      t.state == SchedulerState.REJECTED
    } else {
      t.state == SchedulerState.ACCEPTED
    }

    if (thunks.count(is_paid) == 0) {
      0.00
    } else {
      amount_paid.setScale(2, BigDecimal.RoundingMode.HALF_EVEN) / BigDecimal(thunks.count(is_paid))
    }
  }

  private def info(qinfo: QuestionInfo) : Info = {
    new Info(
      qinfo.name,
      qinfo.question_text,
      qinfo.question_desc,
      qinfo.question_type.toString,
      Utilities.dateToTimestamp(qinfo.start_time),
      qinfo.confidence_level,
      average_thunk_time(qinfo.thunks),
      Double.NaN, // TODO!
      num_answers_received(qinfo.thunks),
      qinfo.total_answers_needed
    )
  }

  private def prevTimeouts(qinfo: QuestionInfo) : Array[PrevTimeout] = {
    qinfo.epochs.zipWithIndex.map { case(e,i) =>
      PrevTimeout(i, Utilities.dateToTimestamp(e.end_time), e.thunks_needed_to_agree, e.prop_that_agree)
    }.toArray
  }

  private def currentTasks(qinfo: QuestionInfo) : Array[CurrentTask] = {
    answered_thunks(qinfo.thunks).zipWithIndex.map { case (t, i) =>
      CurrentTask(
        i,
        Utilities.dateToTimestamp(t.completed_at.get),
        t.answer.get.toString
      )
    }.toArray
  }

  private def budgetInfo(global_budget: BigDecimal, global_budget_used: BigDecimal, qinfo: QuestionInfo) : BudgetInfo = {
    BudgetInfo(
      average_payout(qinfo.dont_reject, qinfo.budget_used, qinfo.thunks).toDouble,
      global_budget.toDouble,
      qinfo.total_budget.toDouble,
      qinfo.budget_used.toDouble,
      Math.min(
        (global_budget - global_budget_used).toDouble,
        (qinfo.total_budget - qinfo.budget_used).toDouble
      )
    )
  }

  def debug_info : Tasks = {
    val state_info: StateInfo = adapter.state_snapshot()

    val global_budget_used = state_info.question_information.foldLeft(BigDecimal(0)){ (acc,t) => acc + t.budget_used}

    // get per-task info
    val tasks = state_info.question_information.map { qinfo =>
        new Task(
          info(qinfo),
          prevTimeouts(qinfo),
          currentTasks(qinfo),
          budgetInfo(state_info.global_budget, global_budget_used, qinfo)
        )
    }.toArray

    Tasks(tasks)
  }
}