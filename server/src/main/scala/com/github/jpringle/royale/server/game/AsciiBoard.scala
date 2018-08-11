package com.github.jpringle.royale.server.game

import com.github.jpringle.royale.common.SnakeProto.GameState
import scala.collection.JavaConverters._

object AsciiBoard {
  private def render(board: Array[Array[Char]]): String = {
    val sb = new StringBuilder
    board.indices.foreach { i =>
      board.indices.foreach { j =>
        sb += board(i)(j)
      }
      sb += '\n'
    }
    sb.result()
  }

  def from(state: GameState): String = {
    val s = state.getBoardSize
    val board = Array.ofDim[Char](s, s)
    board.indices.foreach{i =>
      board.indices.foreach{j =>
        board(i)(j) = '-'
      }
    }
    state.getPlayersList.asScala.foreach{p =>
      board(p.getX)(p.getY) = p.getPlayerId.toString.toCharArray.head
    }

    render(board)
  }
}
