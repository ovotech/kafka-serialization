package com.ovoenergy.serialization.kafka.client.producer

import akka.actor.{ActorRef, ActorRefFactory, PoisonPill}
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.Producer

trait KafkaProducer[K, V] {

  protected def producer: ActorRef

  def produce(event: Event[K, V]): Unit = producer ! event

  /**
    * Stops the producer. After having shut down, this producer cannot be used again.
    * Any message produced after having shut down will be ignored.
    */
  def stop(): Unit = producer ! PoisonPill

}

object KafkaProducer {

  def apply[K, V](config: Config, producerName: String)(implicit system: ActorRefFactory): KafkaProducer[K, V] = new KafkaProducer[K, V] {
    override protected val producer: ActorRef = KafkaProducerClient(config, producerName)
  }

  def apply[K, V](config: ProducerConfig, jProducer: Producer[K, V])(implicit system: ActorRefFactory): KafkaProducer[K, V] = new KafkaProducer[K, V] {
    override protected val producer: ActorRef = KafkaProducerClient(config, jProducer)
  }

}
