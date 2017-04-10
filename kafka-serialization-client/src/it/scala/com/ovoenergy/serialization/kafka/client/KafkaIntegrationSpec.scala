package com.ovoenergy.serialization.kafka.client

import java.util.UUID

import akka.actor.ActorSystem
import com.ovoenergy.serialization.kafka.client.consumer.{ClientId, ConsumerName, KafkaConsumer}
import com.ovoenergy.serialization.kafka.client.producer.{Event, KafkaProducer, ProducerName}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

import scala.concurrent.{Future, Promise}

class KafkaIntegrationSpec extends WordSpec with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures with IntegrationPatience with Matchers with Eventually {

  "start consumer, producer and consume message" in {
    val producerEvent = Event(topic, "key", "value")

    val promise = Promise[Event[String, String]]()

    val subscription = consumer.subscribe { case e =>
      promise.trySuccess(e)
      Future.successful(())
    }

    subscription.futureValue

    producer.produce(producerEvent)

    whenReady(promise.future) { consumerEvent =>
      consumerEvent shouldBe producerEvent
    }
  }

  "restart consumer if subscriber fails" in {
    val eventToSend = Event(topic, "key", "value")

    @volatile var success = false

    consumer.subscribe { case _ =>
      if (success) {
        Future.successful(())
      } else {
        success = true
        Future.failed(new RuntimeException("???"))
      }
    }.futureValue

    producer.produce(eventToSend)

    eventually {
      success shouldBe true
    }

  }

  var topic: Topic = _

  implicit val actorSystem = ActorSystem("integration")

  val config: Config = ConfigFactory.load("integration.conf")

  var consumer: KafkaConsumer[String, String] = _

  var producer: KafkaProducer[String, String] = _

  override protected def beforeEach(): Unit = {
    topic = Topic(randomUUIDString)
    consumer = KafkaConsumer[String, String](config, ConsumerName(randomUUIDString), ClientId("kafka.integration"), topic)
    producer = KafkaProducer[String, String](config, ProducerName(randomUUIDString))
  }

  override protected def afterEach(): Unit = {
    producer.stop()
    consumer.stop()
  }

  override protected def afterAll(): Unit = actorSystem.terminate().futureValue

  private def randomUUIDString = UUID.randomUUID().toString
}
