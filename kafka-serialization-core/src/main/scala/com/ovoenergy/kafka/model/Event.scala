package com.ovoenergy.kafka.model

import java.time.Instant

import com.ovoenergy.kafka.model.Event._

case class Event(topic: String, message: Message)

object Event {

  case class Envelope(eventId: String, traceToken: Option[String], payload: AnyRef, createdAt: Option[Instant] = Some(Instant.now()))

  case class Key(eventType: String, partitionKey: Option[String])

  case class Message(key: Key, value: Envelope)

  def of(topic: String, key: Key, value: Envelope): Event = Event(topic, Message(key, value))

}
