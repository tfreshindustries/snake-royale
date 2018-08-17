package com.github.jpringle.royale.server.graphstage

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.github.jpringle.royale.common.SnakeProto.ClientEvent.{EventCase => EC}
import com.github.jpringle.royale.common.SnakeProto.Direction
import com.github.jpringle.royale.server.Protocol._

import scala.collection.mutable


/**
  *
  */
class PendingEventStage(maxPendingEvents: Int) extends GraphStage[FlowShape[ClientEventWithId, AtomicUpdate]] {
  private val in: Inlet[ClientEventWithId] = Inlet(s"${getClass.getName}.in")
  private val out: Outlet[AtomicUpdate] = Outlet(s"${getClass.getName}.out")

  override def shape: FlowShape[ClientEventWithId, AtomicUpdate] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {
    private val moveBuf: mutable.Map[Int, Vector[Direction]] = mutable.Map.empty
    private val joinBuf: mutable.Map[Int, String] = mutable.Map.empty

    private def enqueueDirection(direction: Direction, playerId: Int): Unit = moveBuf.get(playerId) match {
      case None => moveBuf(playerId) = Vector(direction)
      case Some(xs) if (xs.size < maxPendingEvents) && (xs.last != direction) =>
        moveBuf(playerId) = moveBuf(playerId) :+ direction
      case _ =>
    }

    private def dequeueDirection(playerId: Int): Direction = moveBuf(playerId) match {
      case Vector(direction: Direction) => moveBuf -= playerId; direction
      case buf: Vector[Direction] => moveBuf(playerId) = buf.tail; buf.head
      case _ => throw new IllegalStateException()
    }

    override def preStart(): Unit = {
      pull(in)
      super.preStart()
    }

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        log.debug(s"Player ${elem.id} sent ${elem.event}")
        elem.event.getEventCase match {
          case EC.JOIN_REQUEST => joinBuf(elem.id) = elem.event.getJoinRequest.getPlayerName
          case EC.MOVE_REQUEST => enqueueDirection(elem.event.getMoveRequest.getDirection, elem.id)
          case _ =>
        }
        pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        val join = joinBuf.map { case (id, player) => PendingJoin(id, player) }.toSeq
        val move = moveBuf.map { case (id, _) => PendingMove(id, dequeueDirection(id)) }.toSeq
        push(out, AtomicUpdate(join, move))
        joinBuf.clear()
      }
    })
  }
}
