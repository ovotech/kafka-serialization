package com.ovoenergy.kafka.producer

import akka.actor.{Actor, ActorRef, ActorRefFactory, Cancellable, Props}
import com.ovoenergy.kafka.model.Event
import com.ovoenergy.kafka.model.Event._
import com.ovoenergy.kafka.producer.KafkaProducerClient._
import com.ovoenergy.kafka.producer.Producers._
import com.ovoenergy.kafka.util.ConfigUtils._
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{Producer, ProducerRecord, KafkaProducer => jKafkaProducer}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * A lightweight, non-blocking wrapper around the Apache Kafka Producer class.
  */
private[producer] final class KafkaProducerClient(config: ProducerConfig, producerFactory: () => Producer[Key, Envelope]) extends Actor {

  private implicit val ec = context.system.dispatchers.lookup("kafka.producer.dispatcher")

  private implicit val log = context.system.log

  private val eventQueue = mutable.Queue.empty[Event]

  private var producer: Producer[Key, Envelope] = _

  private var sendEventsJob: Cancellable = _

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, s"Restarting kafka producer: [$message]")
    eventQueue.dequeueAll(_ => true) foreach (event => self ! event)
    super.preRestart(reason, message)
  }

  override def preStart(): Unit = {
    super.preStart()
    log.debug(s"Starting kafka producer with config: [$config]")
    producer = producerFactory()
    sendEventsJob = context.system.scheduler.schedule(config.initialDelay, config.interval, self, Protocol.SendEvents)
  }

  override def receive: Receive = {
    case Protocol.SendEvents =>
      eventQueue.dequeueAll(_ => true) foreach { case event@Event(topic, message) =>
        Future {
          producer.send(new ProducerRecord(topic, message.key, message.value)).get()
        } onFailure { case NonFatal(thrown) =>
          log.error(thrown, s"Publishing [$message] to [$topic] failed!")
          self ! event
        }
      }
    case event: Event => eventQueue.enqueue(event)
  }

  override def postStop(): Unit = {
    producer.closeQuietly
    sendEventsJob.cancel()
    super.postStop()
  }

}

object KafkaProducerClient {

  object Protocol {

    case object SendEvents

  }

  def apply(config: Config)(implicit factory: ActorRefFactory): ActorRef =
    apply(ProducerConfig(config), new jKafkaProducer[Key, Envelope](propertiesFrom(config.getConfig("kafka.producer.properties"))))

  def apply(config: ProducerConfig, producer: Producer[Key, Envelope])(implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(new KafkaProducerClient(config, () => producer)), config.producerName)

}