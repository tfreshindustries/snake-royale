package com.github.jpringle.royale.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory


object ServerMain extends App {
  implicit val system: ActorSystem = ActorSystem("royale")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val conf = ConfigFactory.load()
  val port = conf.getInt("royale.server.port")
  val server = new SnakeRoyaleServer(port)

  server.startAsync().awaitRunning()
}
