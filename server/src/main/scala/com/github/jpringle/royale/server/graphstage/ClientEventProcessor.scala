package com.github.jpringle.royale.server.graphstage

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.github.jpringle.royale.common.SnakeProto.ClientEvent.{EventCase => EC}
import com.github.jpringle.royale.common.SnakeProto.MoveRequest
import com.github.jpringle.royale.server.Protocol.{CandidateMoveWithId, ClientEventWithId}

import scala.collection.mutable

/**
  * Apply some flow control to a stream of incoming client events.
  *
  * Accept incoming client events as fast as they can be produced, buffering the latest move request for
  * each client. When there is demand downstream, emit the sequence of candidate moves, then clear the
  * internal buffer.
  *
  * Even if a (malicious?) client sends a ton of move requests, only the latest is applied per frame.
  */
class ClientEventProcessor extends GraphStage[FlowShape[ClientEventWithId, Seq[CandidateMoveWithId]]] {
  private val in: Inlet[ClientEventWithId] = Inlet("ClientEventProcessor.in")
  private val out: Outlet[Seq[CandidateMoveWithId]] = Outlet("ClientEventProcessor.out")

  override def shape: FlowShape[ClientEventWithId, Seq[CandidateMoveWithId]] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {

    private val buf: mutable.Map[Int, MoveRequest] = mutable.Map.empty

    private def maybePush(): Unit = {
      if (isAvailable(out) && buf.nonEmpty) {
        val seq = buf.map { case (id, moveRequest) => CandidateMoveWithId(id, moveRequest) }.toSeq
        push(out, seq)
        buf.clear()
      }
    }

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        elem.event.getEventCase match {
          case EC.MOVE_REQUEST | EC.JOIN_REQUEST =>
            buf(elem.id) = elem.event.getMoveRequest
            maybePush()
            pull(in)
          case _ =>
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) pull(in)
        maybePush()
      }
    })

  }
}
