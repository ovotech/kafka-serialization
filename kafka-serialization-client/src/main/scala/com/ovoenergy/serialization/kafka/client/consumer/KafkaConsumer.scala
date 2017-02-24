package com.ovoenergy.serialization.kafka.client.consumer

import akka.actor.{ActorRef, ActorRefFactory, PoisonPill}
import akka.pattern._
import akka.util.Timeout
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Protocol.{Done, GetSubscriber, Subscribe}
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Subscriber
import com.ovoenergy.serialization.kafka.client.util.DurationUtils
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.Consumer

import scala.concurrent.Future

trait KafkaConsumer[K, V] {

  protected implicit def askTimeout: Timeout

  protected def consumer: ActorRef

  def subscribe(subscriber: Subscriber[K, V]): Future[Done.type] =
    (consumer ? Subscribe(subscriber)).mapTo[Done.type]

  def subscriber: Future[Option[Subscribe[K, V]]] =
    (consumer ? GetSubscriber).mapTo[Option[Subscribe[K, V]]]

  /**
    * Stops the consumer. After having shut down, this consumer cannot be used again.
    * Any message consumed after having shut down will not be feed to subscriber.
    */
  def stop(): Unit = consumer ! PoisonPill

}

object KafkaConsumer extends DurationUtils {

  def apply[K, V](config: Config, consumerName: ConsumerName, clientId: ClientId, topics: Topic*)(implicit system: ActorRefFactory): KafkaConsumer[K, V] = new KafkaConsumer[K, V] {
    override protected val consumer = KafkaConsumerClient(config, consumerName, clientId, topics: _*)

    override protected implicit def askTimeout: Timeout = getFiniteDuration(config.getString("kafka.consumer.askTimeout"))
  }

  def apply[K, V](config: ConsumerConfig, jConsumer: () => Consumer[K, V])(implicit system: ActorRefFactory): KafkaConsumer[K, V] = new KafkaConsumer[K, V] {
    override protected val consumer = KafkaConsumerClient(config, jConsumer)

    override protected implicit def askTimeout: Timeout = config.askTimeout
  }

}
