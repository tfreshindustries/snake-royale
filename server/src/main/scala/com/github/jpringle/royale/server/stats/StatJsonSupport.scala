package com.github.jpringle.royale.server.stats

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.codahale.metrics.Meter
import spray.json.{DefaultJsonProtocol, PrettyPrinter}

case class StatSnapshot(count: Long, rate1m: Double, rate5m: Double, rate15m: Double, rateMean: Double) {
  def this(m: Meter) = this(
    count = m.getCount,
    rate1m = m.getOneMinuteRate,
    rate5m = m.getFiveMinuteRate,
    rate15m = m.getFifteenMinuteRate,
    rateMean = m.getMeanRate
  )
}

case class OutgoingMessageStats(messagesOut: Map[String, StatSnapshot], bytesOut: Map[String, StatSnapshot])

trait StatRequest

case object OutgoingMessageStatRequest extends StatRequest

trait StatJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val printer = PrettyPrinter
  implicit val statSnapshotFormat = jsonFormat5(StatSnapshot)
  implicit val outgoingMessageStatsFormat = jsonFormat2(OutgoingMessageStats)
}
