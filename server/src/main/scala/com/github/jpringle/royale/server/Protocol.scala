package com.github.jpringle.royale.server

import com.github.jpringle.royale.common.SnakeProto.{ClientEvent, MoveRequest}

object Protocol {
  case class ClientEventWithId(id: Int, event: ClientEvent)
  case class CandidateMoveWithId(id: Int, move: MoveRequest)
}
