package com.github.jpringle.royale.server.game

import com.github.jpringle.royale.common.SnakeProto
import com.github.jpringle.royale.common.SnakeProto.{Direction, GameState, Player}
import com.github.jpringle.royale.server.Protocol._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

case class Point(x: Int, y: Int)

class InternalPlayerState(val id: Int, val name: String, var occupies: List[Point], var direction: Direction) {
  /**
    * Making a 180 is illegal, since the player would immediately crash.
    *
    * @param d new direction
    */
  private def isLegalDirection(d: Direction): Boolean = {
    direction match {
      case Direction.UP if d != Direction.DOWN => true
      case Direction.DOWN if d != Direction.UP => true
      case Direction.LEFT if d != Direction.RIGHT => true
      case Direction.RIGHT if d != Direction.LEFT => true
      case _ => false
    }
  }

  /**
    * Set a new direction for the player.
    *
    * @param d new direction
    */
  def setDirection(d: Direction): Unit = {
    if (isLegalDirection(d)) direction = d
  }

  /**
    * Calculate the next point (move the head of the snake once in the current direction)
    * Put the next point on the head of the list
    * Drop the last element in the list
    */
  def move(): Unit = {
    val hd :: _ = occupies
    val next = direction match {
      case Direction.UP => Point(hd.x, hd.y - 1)
      case Direction.DOWN => Point(hd.x, hd.y + 1)
      case Direction.LEFT => Point(hd.x - 1, hd.y)
      case Direction.RIGHT => Point(hd.x + 1, hd.y)
      case _ => throw new IllegalArgumentException
    }
    occupies = next :: occupies.dropRight(1)
  }
}

/**
  * As usual, you'll find no thread safety here.
  * To use this safely, call [[accept()]] inside a hook from the graphstage
  *
  * @param width  board x
  * @param height board y
  */
class InternalGameState(width: Int, height: Int) {
  private val r = new scala.util.Random()
  private val log = LoggerFactory.getLogger(getClass.getName)
  val playerState: mutable.Map[Int, InternalPlayerState] = mutable.Map.empty

  /**
    * Add a new player to the game, if the player id is not already in use.
    * For now:
    *   - just choose a random spot along the left side of the board to start
    *   - hardcode initial snake size to 4
    *
    * @param j pending join
    */
  private def acceptJoin(j: PendingJoin): Unit = {
    playerState.get(j.playerId) match {
      case Some(_) =>
      case _ =>
        log.info(s"${j.playerName} joined as player ${j.playerId}")
        val y = r.nextInt(height)
        val occupies = Point(0, y) :: Point(-1, y) :: Point(-2, y) :: Point(-3, y) :: Nil
        playerState(j.playerId) = new InternalPlayerState(j.playerId, j.playerName, occupies, Direction.RIGHT)
    }
  }

  /**
    * Possibly change the direction of an active player
    *
    * @param m candidate move
    */
  private def acceptMove(m: PendingMove): Unit = {
    playerState.get(m.playerId) match {
      case Some(s) => s.setDirection(m.direction)
      case _ =>
    }
  }

  /**
    * If the head of any snake overlaps with another point, remove the player.
    * This can probably be done more efficiently with a little thought
    */
  private def handlePlayerCollisions(): Unit = {
    val occupied = playerState.values.flatMap(_.occupies)
    val crashed = playerState.flatMap {
      case (id, state) =>
        if (occupied.count(_ == state.occupies.head) != 1) Some(id)
        else None
    }
    crashed.foreach { id =>
      log.info(s"player $id crashed into another player!")
      playerState -= id
    }
  }

  /**
    * If the head of any snake leaves the board, remove the player
    */
  private def handleBoardCollisions(): Unit = {
    val crashed = playerState.flatMap {
      case (id, state) =>
        val hd = state.occupies.head
        if (hd.x < 0 | hd.x >= width | hd.y < 0 | hd.y >= height) Some(id)
        else None

    }
    crashed.foreach { id =>
      log.info(s"player $id crashed into the edge of the board!")
      playerState -= id
    }
  }

  private def handleEvents(): Unit = {
    handlePlayerCollisions()
    handleBoardCollisions()
  }

  private def buildGameState(): GameState = {
    val protoPlayers = playerState.map {
      case (id, state) =>
        val occupies = state.occupies
          .filter(p => p.x >= 0 && p.y >= 0)
          .map { p => SnakeProto.Point.newBuilder().setX(p.x).setY(p.y).build() }

        Player.newBuilder()
          .setPlayerId(id)
          .addAllOccupies(occupies.asJava)
          .build()
    }
    // TODO some weirdness around casting a scala Int to a java Integer
    val idToName = playerState.map { case (id, state) => (new Integer(id), state.name) }.toMap
    GameState.newBuilder()
      .setBoardHeight(height)
      .setBoardWidth(width)
      .addAllPlayers(protoPlayers.asJava)
      .putAllIdToName(idToName.asJava)
      .build()
  }

  /**
    * Apply any pending changes, then move the game state forward by one frame.
    *
    * @param upd changes to be applied atomically
    * @return
    */
  def accept(upd: AtomicUpdate): GameState = {
    upd.join.foreach(acceptJoin)
    upd.move.foreach(acceptMove)
    playerState.values.foreach(_.move())
    handleEvents()
    buildGameState()
  }
}
