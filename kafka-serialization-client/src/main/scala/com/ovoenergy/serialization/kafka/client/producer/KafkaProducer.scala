package com.ovoenergy.serialization.kafka.client.producer

import akka.actor.{ActorRef, ActorRefFactory}
import com.ovoenergy.serialization.kafka.client.model.Event
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.Producer

trait KafkaProducer {

  protected def producer: ActorRef

  def produce(event: Event): Unit = producer ! event

}

object KafkaProducer {

  def apply(config: Config)(implicit system: ActorRefFactory): KafkaProducer = new KafkaProducer {
    override protected val producer: ActorRef = KafkaProducerClient(config)
  }

  def apply(config: ProducerConfig, jProducer: Producer[Event.Key, Event.Envelope])(implicit system: ActorRefFactory): KafkaProducer = new KafkaProducer {
    override protected val producer: ActorRef = KafkaProducerClient(config, jProducer)
  }

}
