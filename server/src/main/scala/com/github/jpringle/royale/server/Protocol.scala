package com.github.jpringle.royale.server

import com.github.jpringle.royale.common.SnakeProto.{ClientEvent, Direction, MoveRequest}

object Protocol {
  case class ClientEventWithId(id: Int, event: ClientEvent)
  case class CandidateMoveWithId(id: Int, move: MoveRequest)

  trait PendingEvent {
    val playerId: Int
  }
  case class PendingMove(playerId: Int, direction: Direction) extends PendingEvent
  case class PendingJoin(playerId: Int, playerName: String) extends PendingEvent
  case class AtomicUpdate(join: Seq[PendingJoin], move: Seq[PendingMove])
}
