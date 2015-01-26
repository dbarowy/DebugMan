package edu.umass.cs.plasma.automandebugger

import edu.umass.cs.automan.core.{AutomanAdapter, Plugin}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import scala.concurrent.duration._
import akka.actor.{ActorRef, Props, ActorSystem}

object AutomanDebugger {
  def plugin = classOf[AutomanDebugger]
}

class AutomanDebugger extends Plugin {
  implicit var _actor_system: ActorSystem = _
  var _debugger_actor: ActorRef = _
  
  def startup(adapter: AutomanAdapter) {
    // init actor system
    _actor_system = ActorSystem("on-spray-can")

    // actor properties
    val props = Props(new DebugServer(adapter))

    // init debugger actor
    _debugger_actor = _actor_system.actorOf(props, "debugger-service")

    // set timeout implicit for ? (ask)
    implicit val timeout = akka.util.Timeout(5.seconds)

    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(_debugger_actor, interface = "localhost", port = 8080)
  }
  
  def shutdown() {
    _actor_system.shutdown()
  }
}