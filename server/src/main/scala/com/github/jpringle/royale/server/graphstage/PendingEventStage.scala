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
class PendingEventStage extends GraphStage[FlowShape[ClientEventWithId, AtomicUpdate]] {
  private val in: Inlet[ClientEventWithId] = Inlet(s"${getClass.getName}.in")
  private val out: Outlet[AtomicUpdate] = Outlet(s"${getClass.getName}.out")

  override def shape: FlowShape[ClientEventWithId, AtomicUpdate] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {
    private val moveBuf: mutable.Map[Int, Direction] = mutable.Map.empty
    private val joinBuf: mutable.Map[Int, String] = mutable.Map.empty

    override def preStart(): Unit = {
      pull(in)
      super.preStart()
    }

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        log.info(s"Player ${elem.id} sent ${elem.event}")
        elem.event.getEventCase match {
          case EC.JOIN_REQUEST => joinBuf(elem.id) = elem.event.getJoinRequest.getPlayerName
          case EC.MOVE_REQUEST => moveBuf(elem.id) = elem.event.getMoveRequest.getDirection
          case _ =>
        }
        pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        val join = joinBuf.map { case (id, player) => PendingJoin(id, player) }.toSeq
        val move = moveBuf.map { case (id, direction) => PendingMove(id, direction) }.toSeq
        val foo = AtomicUpdate(join, move)
        push(out, foo)
        moveBuf.clear()
        joinBuf.clear()
      }
    })
  }
}
