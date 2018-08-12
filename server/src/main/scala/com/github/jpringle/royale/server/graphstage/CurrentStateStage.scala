package com.github.jpringle.royale.server.graphstage

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.github.jpringle.royale.common.SnakeProto.ServerEvent
import com.github.jpringle.royale.server.Protocol.AtomicUpdate
import com.github.jpringle.royale.server.game.InternalGameState

class CurrentStateStage(width: Int, height: Int) extends GraphStage[FlowShape[AtomicUpdate, ServerEvent]] {
  private val in: Inlet[AtomicUpdate] = Inlet(s"${getClass.getName}.in")
  private val out: Outlet[ServerEvent] = Outlet(s"${getClass.getName}.out")

  override def shape: FlowShape[AtomicUpdate, ServerEvent] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {

    val state = new InternalGameState(width, height)

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        val next = state.accept(elem)
        val event = ServerEvent.newBuilder().setGameState(next).build()
        push(out, event)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })

  }
}
