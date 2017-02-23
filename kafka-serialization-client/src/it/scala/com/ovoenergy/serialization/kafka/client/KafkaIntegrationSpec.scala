package com.ovoenergy.serialization.kafka.client

import java.util.UUID

import akka.actor.ActorSystem
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumer
import com.ovoenergy.serialization.kafka.client.producer.{Event, KafkaProducer}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Created by Piotr Fras on 23/02/17.
  */
class KafkaIntegrationSpec extends WordSpec with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures with IntegrationPatience with Matchers {

  "start consumer, producer and consume message" in {
    val producerEvent = Event(topic, "key", "value")

    val promise = Promise[Event[String, String]]()

    val subscription = consumer.subscribe { case e =>
      promise.trySuccess(e)
      Future.successful(())
    }

    subscription.futureValue

    producer.produce(producerEvent)

    val consumerEvent = Await.result(promise.future, Duration.Inf)

    whenReady(promise.future) { consumerEvent =>
      consumerEvent shouldBe producerEvent
    }
  }

  var topic: String = _

  implicit val actorSystem = ActorSystem("integration")

  val config: Config = ConfigFactory.load("integration.conf")

  var consumer: KafkaConsumer[String, String] = _

  var producer: KafkaProducer[String, String] = _

  override protected def beforeEach(): Unit = {
    topic = UUID.randomUUID().toString
    consumer = KafkaConsumer[String, String](config, "kafka.integration.consumer", topic)
    producer = KafkaProducer[String, String](config, "kafka.integration.producer")
  }

  override protected def afterEach(): Unit = {
    producer.stop()
    consumer.stop()
  }

  override protected def afterAll(): Unit = actorSystem.terminate().futureValue
}
