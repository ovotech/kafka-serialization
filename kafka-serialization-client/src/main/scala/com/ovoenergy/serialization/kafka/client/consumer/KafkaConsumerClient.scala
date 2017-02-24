package com.ovoenergy.serialization.kafka.client.consumer

import akka.actor.{Actor, ActorRef, ActorRefFactory, Cancellable, Props}
import akka.pattern._
import com.ovoenergy.serialization.kafka.client.consumer.Consumers._
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Protocol
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Protocol.Subscribe
import com.ovoenergy.serialization.kafka.client.producer.Event
import com.ovoenergy.serialization.kafka.client.util.ConfigUtils._
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.{Consumer, ConsumerRecords, KafkaConsumer => JavaKafkaConsumer}

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
  * A lightweight, non-blocking wrapper around the Apache Kafka Consumer class.
  *
  * The client does not handle errors.
  */
private[consumer] final class KafkaConsumerClient[K, V](config: ConsumerConfig, consumerFactory: () => Consumer[K, V]) extends Actor {

  private implicit val log = context.system.log

  private implicit val ec = context.system.dispatchers.lookup("kafka.consumer.dispatcher")

  private var consumer: Consumer[K, V] = _

  private var subscriber: Option[Subscribe[K, V]] = None

  private var pollJob: Cancellable = _

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, s"Restarting kafka consumer: [$message]")
    subscriber.foreach(subscription => self ! subscription)
    super.preRestart(reason, message)
  }

  override def preStart(): Unit = {
    super.preStart()
    log.debug(s"Starting kafka consumer with config: [$config]")
    consumer = consumerFactory()
    pollJob = context.system.scheduler.schedule(config.initialDelay, config.initialDelay, self, KafkaConsumerClient.Protocol.Poll(config.pollingTimeout))
  }

  override def receive: Receive = polling

  private def polling: Receive = subscribing orElse {
    case Protocol.Poll(timeout) if subscriber.nonEmpty =>
      val records = consumer.poll(timeout)
      if (records.nonEmpty) {
        context.become(consuming(records))
      }
    case _: Protocol.Poll =>
  }

  private def consuming(records: ConsumerRecords[K, V]): Receive = {
    feedSubscribers(records, subscriber.toSeq).pipeTo(self)
    subscribing orElse failing orElse {
      case _: Protocol.Poll =>
      case Protocol.Done =>
        Future {
          consumer.commitSync()
        }.map(_ => Protocol.Done).pipeTo(self)
        context.become(committing)
    }
  }

  private def committing: Receive = subscribing orElse failing orElse {
    case Protocol.Done => context.become(polling)
  }

  private def failing: Receive = {
    case akka.actor.Status.Failure(thrown) => throw thrown
  }

  private def subscribing: Receive = {
    case sub: Protocol.Subscribe[K, V] =>
      consumer.subscribe(config.topics.map(_.value))
      subscriber = Some(sub)
      sender() ! Protocol.Done
    case Protocol.GetSubscriber => sender() ! subscriber
  }

  override def postStop(): Unit = {
    consumer.closeQuietly
    pollJob.cancel()
    super.postStop()
  }

}

object KafkaConsumerClient {

  type Subscriber[K, V] = PartialFunction[Event[K, V], Future[Unit]]

  object Protocol {

    case class Subscribe[K, V](value: Subscriber[K, V])

    case class Poll(timeout: Long)

    case object GetSubscriber

    case object Done

  }

  def apply[K, V](config: Config, consumerName: ConsumerName, clientId: ClientId, topics: Topic*)(implicit system: ActorRefFactory): ActorRef = {
    val consumerProperties = propertiesFrom(config.getConfig("kafka.consumer.properties"))
    apply(ConsumerConfig(config, consumerName, clientId, topics: _*), () => new JavaKafkaConsumer[K, V](consumerProperties))
  }

  def apply[K, V](config: ConsumerConfig, consumer: () => Consumer[K, V])(implicit system: ActorRefFactory): ActorRef =
    system.actorOf(Props(new KafkaConsumerClient(config, consumer)), config.consumerName.value)

}
