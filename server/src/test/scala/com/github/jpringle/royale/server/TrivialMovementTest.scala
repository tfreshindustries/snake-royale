package com.github.jpringle.royale.server

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.util.ByteString
import com.github.jpringle.royale.common.SnakeProto.JoinResponse.{ResponseCase => RC}
import com.github.jpringle.royale.common.SnakeProto.ServerEvent.{EventCase => EC}
import com.github.jpringle.royale.common.SnakeProto.{ClientEvent, JoinRequest, ServerEvent}
import org.scalatest.{FlatSpec, Matchers}

class TrivialMovementTest extends FlatSpec with ScalatestRouteTest with Matchers {
  val server = new SnakeRoyaleServer(0)
  val wsClient = WSProbe()

  "A JoinRequest" should "emit a JoinResponse" in {
    WS("/ws", wsClient.flow) ~> server.route ~> check {
      isWebSocketUpgrade shouldEqual true
      val joinReq = JoinRequest.newBuilder().setPlayerName("Mario").build()
      val event = ClientEvent.newBuilder().setJoinRequest(joinReq).build()
      wsClient.sendMessage(ByteString(event.toByteArray))
      val resp = wsClient.expectMessage().asBinaryMessage
      val ev = ServerEvent.parseFrom(resp.getStrictData.toByteBuffer)
      ev.getEventCase match {
        case EC.JOIN_RESPONSE =>
          ev.getJoinResponse.getResponseCase match {
            case RC.JOIN_SUCCESS =>
              val joinSuccess = ev.getJoinResponse.getJoinSuccess
              joinSuccess.getPlayerId shouldEqual 1
            case x => throw new AssertionError(s"Did not receive a JoinSuccess (got $x)")
          }
        case x => throw new AssertionError(s"Did not receive a JoinResponse (got $x)")
      }

      (0 until 10).foreach { i =>
        val resp = wsClient.expectMessage().asBinaryMessage
        val ev = ServerEvent.parseFrom(resp.getStrictData.toByteBuffer)
        ev.getEventCase shouldEqual EC.GAME_STATE
        println(ev.toString)
        println("---")
      }
    }
  }
}
