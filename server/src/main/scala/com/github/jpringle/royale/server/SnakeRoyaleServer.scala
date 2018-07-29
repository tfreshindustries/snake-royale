package com.github.jpringle.royale.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.google.common.util.concurrent.AbstractService

import scala.util.{Failure, Success}

class SnakeRoyaleServer(port: Int, contentRoot: String)(implicit val system: ActorSystem, m: ActorMaterializer) extends AbstractService {
  private var binding: ServerBinding = _
  private val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)


  private val route = path("ping") {
    complete("pong!")
  } ~ pathPrefix("") {
    getFromDirectory(contentRoot)
  }

  override def doStart(): Unit = {
    Http().bindAndHandle(route, "0.0.0.0", port)
      .onComplete {
        case Success(b) =>
          binding = b
          log.info(s"Running on ${b.localAddress}")
        case Failure(t) =>
          log.error(t.toString)
          System.exit(1)
      }(m.executionContext)
  }

  override def doStop(): Unit = {
    binding.unbind()
  }
}
