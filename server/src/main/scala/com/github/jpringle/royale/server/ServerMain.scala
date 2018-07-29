package com.github.jpringle.royale.server

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object ServerMain extends App {
  implicit val system: ActorSystem = ActorSystem("royale")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val conf = ConfigFactory.load()
  val port = conf.getInt("royale.server.port")
  val contentRoot = Paths.get(conf.getString("royale.server.content-root")).toAbsolutePath.toString
  val server = new SnakeRoyaleServer(port, contentRoot)

  server.startAsync()
}
