package com.github.jpringle.royale.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object ServerMain extends App {
  implicit val system: ActorSystem = ActorSystem("royale")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val port = 12345

  val royaleServer = new SnakeRoyaleServer(port)
  royaleServer.startAsync()
}
