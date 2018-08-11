package com.github.jpringle.royale.server.graphstage

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.github.jpringle.royale.common.SnakeProto.ClientEvent.{EventCase => EC}
import com.github.jpringle.royale.common.SnakeProto.{Direction, GameState, PlayerSnapshot}
import com.github.jpringle.royale.server.Protocol.{CandidateMoveWithId, ClientEventWithId}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Apply a sequence of candidate moves, then emit the current game state.
  */

case class Position(x: Int, y: Int)

class MoveProcessor extends GraphStage[FlowShape[Seq[ClientEventWithId], GameState]] {
  private val in: Inlet[Seq[ClientEventWithId]] = Inlet("MoveProcessor.in")
  private val out: Outlet[GameState] = Outlet("MoveProcessor.out")

  override def shape: FlowShape[Seq[ClientEventWithId], GameState] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {

    private val boardSize = 50 //TODO
    private val playerPositions: mutable.Map[Int, Position] = mutable.Map.empty
    private val playerDirections: mutable.Map[Int, Direction] = mutable.Map.empty

    private def computeNextFrame(xs: Seq[CandidateMoveWithId]): GameState = {
      xs.foreach { x =>
        // Add new players if necessary
        playerPositions.get(x.id) match {
          case None => playerPositions(x.id) = Position(25, 25) // TODO - find somewhere open
          case _ =>
        }
        // Update any changed player directions
        playerDirections(x.id) = x.move.getDirection
      }

      // Move everyone
      playerDirections.foreach {
        case (id, d) =>
          val prev = playerPositions(id)
          playerPositions(id) = d match {
            case Direction.UP => prev.copy(y = prev.y + 1)
            case Direction.DOWN => prev.copy(y = prev.y - 1)
            case Direction.RIGHT => prev.copy(y = prev.x + 1)
            case Direction.LEFT => prev.copy(x = prev.x - 1)
            case _ => prev
          }
      }

      // Check if anyone left the board (no collision checks yet)
      playerPositions.keySet.toSeq.foreach { id =>
        val pos = playerPositions(id)
        if (pos.x < 0 || pos.x >= boardSize || pos.y < 0 || pos.y >= boardSize) {
          log.info(s"Player $id ded")
          playerPositions -= id
          playerDirections -= id
        }
      }

      val allPlayers = playerPositions.toSeq.map {
        case (id, pos) =>
          PlayerSnapshot.newBuilder()
            .setPlayerId(id)
            .setX(pos.x)
            .setY(pos.y)
            .build()
      }

      GameState.newBuilder()
        .addAllPlayers(allPlayers.asJava)
        .setBoardSize(boardSize)
        .build()
    }

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val events = grab(in)
        // Handle join requests first
        events.foreach { ev =>
          ev.event.getEventCase match {
            case EC.JOIN_REQUEST =>
              log.info(s"Join request for player ${ev.id}")
              playerPositions(ev.id) = Position(25, 25)
              playerDirections(ev.id) = Direction.LEFT
            case _ =>
          }
        }
        // then handle move requests
        val candidateMoves = events
          .filter(_.event.getEventCase == EC.MOVE_REQUEST)
          .map { x => CandidateMoveWithId(x.id, x.event.getMoveRequest) }

        val frame = computeNextFrame(candidateMoves)
        push(out, frame)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })

  }
}
