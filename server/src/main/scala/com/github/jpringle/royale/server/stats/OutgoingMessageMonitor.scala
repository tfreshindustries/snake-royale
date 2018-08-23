package com.github.jpringle.royale.server.stats

import akka.actor.{Actor, ActorLogging, Props}
import com.codahale.metrics.Meter
import com.github.jpringle.royale.common.SnakeProto.ServerEvent

import scala.collection.mutable
import com.github.jpringle.royale.common.SnakeProto.ServerEvent.{EventCase => EC}
import com.google.protobuf.GeneratedMessageV3

/**
  * Track outgoing messages/bytes by message case
  */
class OutgoingMessageMonitor extends Actor with ActorLogging {
  private val messagesOut: mutable.Map[EC, Meter] = mutable.Map.empty
  private val bytesOut: mutable.Map[EC, Meter] = mutable.Map.empty

  /**
    * Accept a single message and record its category and size.
    *
    * This does mean we encode each message twice, but since the volume is so low it's not
    * really a concern.
    *
    * @param key  event case
    * @param elem any proto message
    */
  private def accept(key: EC, elem: GeneratedMessageV3): Unit = {
    messagesOut.get(key) match {
      case None => messagesOut(key) = new Meter
      case _ =>
    }
    bytesOut.get(key) match {
      case None => bytesOut(key) = new Meter
      case _ =>
    }
    messagesOut(key).mark()
    bytesOut(key).mark(elem.toByteArray.length)
  }

  /**
    * Take a snapshot of the current outgoing message stats
    *
    * @return
    */
  private def getCurrentStats: OutgoingMessageStats = {
    val mOut = messagesOut.map { case (ec, m) => (ec.toString, new StatSnapshot(m)) }.toMap
    val bOut = bytesOut.map { case (ec, m) => (ec.toString, new StatSnapshot(m)) }.toMap
    OutgoingMessageStats(mOut, bOut)
  }

  private def handleServerEvent(event: ServerEvent): Unit = event.getEventCase match {
    case EC.GAME_STATE =>
      accept(EC.GAME_STATE, event.getGameState)
    case EC.JOIN_RESPONSE =>
      accept(EC.JOIN_RESPONSE, event.getJoinResponse)
    case _ =>
  }

  override def receive: Receive = {
    case OutgoingMessageStatRequest => context.sender ! getCurrentStats
    case e: ServerEvent => handleServerEvent(e)
    case _ =>
  }
}

object OutgoingMessageMonitor {
  def props: Props = Props(new OutgoingMessageMonitor)
}
