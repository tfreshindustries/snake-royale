package com.github.jpringle.royale.server

import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.http.scaladsl.model.{HttpRequest, RemoteAddress}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.github.jpringle.royale.common.SnakeProto.{ClientEvent, JoinResponse, JoinSuccess, ServerEvent}
import com.github.jpringle.royale.server.Protocol.{AtomicUpdate, ClientEventWithId}
import com.github.jpringle.royale.server.graphstage.{CurrentStateStage, PendingEventStage}
import com.google.common.util.concurrent.AbstractService

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class SnakeRoyaleServer(port: Int, contentRoot: String)(implicit val system: ActorSystem, m: ActorMaterializer) extends AbstractService {
  private var binding: ServerBinding = _
  private val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)
  private val idGen = new AtomicInteger(1)
  private val mergeHub = MergeHub.source[ClientEventWithId](perProducerBufferSize = 16)
  private val broadcastHub = BroadcastHub.sink[ServerEvent](bufferSize = 256)
  private val (eventSink, eventSource) = mergeHub.preMaterialize()
  private val (broadcastSource, broadcastSink) = broadcastHub.preMaterialize()

  // drain events when there are no subscribers
  broadcastSource.runWith(Sink.ignore)

  private val tickRate: FiniteDuration = 75.millis

  Source.tick(0.seconds, tickRate, akka.NotUsed)
    .zipWith(eventSource.via(new PendingEventStage))(Keep.right)
    .keepAlive(tickRate, () => AtomicUpdate(Seq.empty, Seq.empty))
    .via(new CurrentStateStage(width = 128, height = 72))
    .runWith(broadcastSink)

  def toStrict(message: Message): ByteString = message match {
    case BinaryMessage.Strict(data) => data
    case BinaryMessage.Streamed(dataStream) =>
      dataStream.runWith(Sink.cancelled)
      throw new IllegalArgumentException("Only strict binary messages are supported")
    case _ => throw new IllegalArgumentException("Only strict binary messages are supported")
  }

  def flow: Flow[Message, Message, NotUsed] = {
    val playerId = idGen.getAndIncrement()
    val joinResponseSource = {
      val joinSuccess = JoinSuccess.newBuilder().setPlayerId(playerId).build()
      val joinResponse = JoinResponse.newBuilder().setJoinSuccess(joinSuccess)
      val serverEvent = ServerEvent.newBuilder().setJoinResponse(joinResponse).build()
      Source.single(serverEvent)
    }
    val sink = Flow[Message]
      .map(toStrict)
      .map(bs => ClientEvent.parseFrom(bs.asByteBuffer))
      .map { event => ClientEventWithId(playerId, event) }
      .to(eventSink)
    val source = joinResponseSource.concat(broadcastSource)
      .map { event => BinaryMessage.Strict(ByteString.fromArray(event.toByteArray)) }

    Flow.fromSinkAndSource(sink, source).backpressureTimeout(1.second)
  }

  val route: Route = path("ping") {
    complete("pong!")
  } ~ path("ws") {
    handleWebSocketMessages(flow)
  } ~ path("" ~ PathEnd) {
    getFromDirectory(Paths.get(contentRoot, "index.html").toAbsolutePath.toString)
  } ~
    pathPrefix("") {
      getFromDirectory(contentRoot)
    }

  private def logRequestResult(route: Route, ip: RemoteAddress)(implicit m: Materializer): Route = {
    def doLog(adapter: LoggingAdapter)(req: HttpRequest)(res: Any): Unit = {
      val entry = res match {
        case Complete(resp) => Some(s"[$ip] ${req.method} ${req.uri} : ${resp.status}")
        case Rejected(reasons) => Some(s"[$ip] REJECTED $reasons")
        case _ => None
      }
      entry.foreach(e => LogEntry(e, Logging.InfoLevel).logTo(adapter))
    }

    DebuggingDirectives.logRequestResult(LoggingMagnet(adapter => doLog(adapter)))(route)
  }

  override def doStart(): Unit = {
    val loggedRoute = extractClientIP { ip =>
      logRequestResult(route, ip)
    }
    Http().bindAndHandle(loggedRoute, "0.0.0.0", port)
      .onComplete {
        case Success(b) =>
          binding = b
          log.info(s"Running on ${b.localAddress}")
          notifyStarted()
        case Failure(t) =>
          log.error(t.toString)
          System.exit(1)
      }(m.executionContext)
  }

  override def doStop(): Unit = {
    binding.unbind()
    notifyStopped()
  }
}
