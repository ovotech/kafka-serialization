package com.ovoenergy.serialization.kafka.client.producer

import akka.actor.{ActorRef, ActorRefFactory}
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.Producer

trait KafkaProducer[K, V] {

  protected def producer: ActorRef

  def produce(event: Event[K, V]): Unit = producer ! event

}

object KafkaProducer {

  def apply[K, V](config: Config)(implicit system: ActorRefFactory): KafkaProducer[K, V] = new KafkaProducer[K, V] {
    override protected val producer: ActorRef = KafkaProducerClient(config)
  }

  def apply[K, V](config: ProducerConfig, jProducer: Producer[K, V])(implicit system: ActorRefFactory): KafkaProducer[K, V] = new KafkaProducer[K, V] {
    override protected val producer: ActorRef = KafkaProducerClient(config, jProducer)
  }

}
