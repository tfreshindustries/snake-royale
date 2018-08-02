package scratch

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, TimerGraphStageLogic}
import akka.stream.{ActorMaterializer, Attributes, Outlet, SourceShape}
import com.github.jpringle.royale.common.SnakeProto.{ClientEvent, Direction, MoveRequest, ServerEvent}
import com.github.jpringle.royale.server.Protocol.ClientEventWithId
import com.github.jpringle.royale.server.graphstage.{ClientEventProcessor, MoveProcessor}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Random

/**
  * Produces a random move every 0-100ms
  */
class RandomMoveSource extends GraphStage[SourceShape[ClientEvent]] {
  private val out: Outlet[ClientEvent] = Outlet(s"${getClass.getName}.out")

  override def shape: SourceShape[ClientEvent] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {

    val r = new Random
    val directions = Seq(Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN)

    override def onTimer(timerKey: Any): Unit = {
      val elem = directions(Random.nextInt(directions.length))
      val moveRequest = MoveRequest.newBuilder().setDirection(elem).build()
      val event = ClientEvent.newBuilder().setMoveRequest(moveRequest).build()
      push(out, event)
    }

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        val delay = Random.nextInt(100).millis
        scheduleOnce(None, delay)
      }
    })
  }
}


object PubSubExample extends App {
  implicit val system: ActorSystem = ActorSystem("royale")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val (eventSink, eventSource) = MergeHub.source[ClientEventWithId].preMaterialize()
  val (broadcastSource, broadcastSink) = BroadcastHub.sink[ServerEvent].preMaterialize()

  val eventProcessor = Flow.fromGraph(new ClientEventProcessor)
  val moveProcessor = Flow.fromGraph(new MoveProcessor)

  val unthrottledEventSource = eventSource.via(eventProcessor)

  // This test event source produces elements much faster than the game's tickrate, so throttle it here
  // Only feed events into moveProcessor at the specified tickrate
  Source.tick(0.seconds, 500.millis, akka.NotUsed)
    .zipWith(unthrottledEventSource)(Keep.right)
    .via(moveProcessor)
    .map(state => ServerEvent.newBuilder().setGameState(state).build())
    .runWith(broadcastSink)

  // Initialize some number of players
  (0 until 5).foreach { consumerId =>

    // Send random moves for the player into the event sink
    Source
      .fromGraph(new RandomMoveSource)
      .map(event => ClientEventWithId(consumerId, event))
      .runWith(eventSink)

    // Subscribe to the broadcast source
    // All clients should get the same messages
    broadcastSource.runForeach { event =>
      val players = event.getGameState.getPlayersList.asScala
      val message = players.map { ps =>
        val id = ps.getPlayerId
        val x = ps.getX
        val y = ps.getY
        s"Player $id is at ($x, $y)"
      }.mkString(" | ")
      if (consumerId == 0) println(s"[Consumer $consumerId] $message")
    }
  }

}
