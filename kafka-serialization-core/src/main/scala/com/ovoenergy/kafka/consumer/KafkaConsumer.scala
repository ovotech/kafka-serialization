package com.ovoenergy.kafka.consumer

import akka.actor.{ActorRef, ActorRefFactory}
import akka.pattern._
import akka.util.Timeout
import com.ovoenergy.kafka.consumer.KafkaConsumerClient.Protocol.{Done, GetSubscriber, Subscribe}
import com.ovoenergy.kafka.consumer.KafkaConsumerClient.Subscriber
import com.ovoenergy.kafka.model.Event.{Envelope, Key}
import com.ovoenergy.kafka.util.DurationUtils
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.Consumer

import scala.concurrent.Future

trait KafkaConsumer {

  protected implicit def askTimeout: Timeout

  protected def consumer: ActorRef

  def subscribe(subscriber: Subscriber): Future[Done.type] =
    (consumer ? Subscribe(subscriber)).mapTo[Done.type]

  def subscriber: Future[Option[Subscribe]] =
    (consumer ? GetSubscriber).mapTo[Option[Subscribe]]

}

object KafkaConsumer extends DurationUtils {

  def apply(config: Config, topic: String, clientId: String)(implicit system: ActorRefFactory): KafkaConsumer = new KafkaConsumer {
    override protected val consumer = KafkaConsumerClient(config, topic, clientId)

    override protected implicit def askTimeout: Timeout = getFiniteDuration(config.getString("kafka.consumer.askTimeout"))
  }

  def apply(config: ConsumerConfig, jConsumer: Consumer[Key, Envelope])(implicit system: ActorRefFactory): KafkaConsumer = new KafkaConsumer {
    override protected val consumer = KafkaConsumerClient(config, jConsumer)

    override protected implicit def askTimeout: Timeout = config.askTimeout
  }

}
