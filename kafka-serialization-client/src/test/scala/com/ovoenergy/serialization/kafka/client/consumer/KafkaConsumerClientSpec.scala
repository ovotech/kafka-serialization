package com.ovoenergy.serialization.kafka.client.consumer

import java.util

import com.ovoenergy.serialization.kafka.client.ActorSpecContext
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Subscriber
import com.ovoenergy.serialization.kafka.client.model.Event.{Envelope, Key}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.{Success, Try}

class KafkaConsumerClientSpec extends Specification with Mockito with ScalaFutures with PatienceConfiguration {

  "KafkaConsumerClientSpec" should {

    "subscribe to topics" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns ConsumerRecords.empty()

      val client = consumerClient("test1", "subscribe-to-topic", "subscribe-client-id")
      client.subscribe(success).futureValue

      eventually {
        there was one(consumer).subscribe(Seq("subscribe-to-topic", "group.id.subscribe-client-id.requeued"))
      }
    }

    "poll for messages" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns ConsumerRecords.empty()

      val client = consumerClient("test2", "poll-for-message-topic", "poll-for-message-client-id")
      client.subscribe(success).futureValue

      eventually {
        there was atLeastOne(consumer).poll(pollingTimeout)
      }
    }

    "let subscribers subscribe to configured topic" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns ConsumerRecords.empty()

      val client = consumerClient("test3", "topic", "clientId")

      client.subscribe(success).futureValue

      whenReady(client.subscriber) { subscriber =>
        subscriber should not be empty
      }

    }

    "recycle existing subscriber if restarted" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns records(record("recycle-topic-1"))

      val client = consumerClient("test4", "recycle-topic-1", "recycle-client-id-1")

      client.subscribe(naughtySubscriber).futureValue

      eventually {
        whenReady(client.subscriber) { subscriber =>
          subscriber should not be empty
        }
      }

      eventually {
        there was atLeastOne(consumer).poll(pollingTimeout)
      }

      eventually {
        there was atLeastOne(consumer).close()
      }

      eventually {
        there was atLeastTwo(consumer).poll(pollingTimeout)
      }

    }

    "commit offset if subscriber consumed message" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns records(record("commit-topic-1"))

      val client = consumerClient("test6", "commit-topic-1", "commit-client-id")

      client.subscribe(success).futureValue


      whenReady(client.subscriber) { subscriber =>
        subscriber should not be empty
      }

      eventually {
        there was one(consumer).commitSync()
      }
    }

    "restart offset if subscriber failed" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns records(record("restart-topic-1"))

      val client = consumerClient("test7", "restart-topic-1", "restart-client-id")

      client.subscribe(naughtySubscriber).futureValue

      whenReady(client.subscriber) { subscriber =>
        subscriber should not be empty
      }

      eventually {
        there was no(consumer).commitSync()
      }

    }

  }

  trait KafkaConsumerClientSpecContext extends ActorSpecContext {
    val consumer = mock[Consumer[Try[Key], Try[Envelope]]]
    val pollingTimeout = 1000L

    def record(topic: String) = new ConsumerRecord[Try[Key], Try[Envelope]](topic, 0, 0L, Success(Key("eventType", None)), Success(Envelope("eventId", None, "{}")))

    def records(values: ConsumerRecord[Try[Key], Try[Envelope]]*): ConsumerRecords[Try[Key], Try[Envelope]] = {
      new ConsumerRecords[Try[Key], Try[Envelope]](values.groupBy(_.topic()).map { case (topic, list) =>
        new TopicPartition(topic, 0) -> util.Arrays.asList(list: _*)
      })
    }

    val success: Subscriber = {
      case _ => Future.successful((): Unit)
    }

    val failure: Subscriber = {
      case _ => Future.failed(new RuntimeException)
    }

    val naughtySubscriber: Subscriber = {
      case _ => throw new RuntimeException
    }

    def consumerClient(consumerName: String, topic: String, clientId: String): KafkaConsumer = KafkaConsumer(config(consumerName, topic, clientId), () => consumer)

    def config(consumerName: String, topic: String, clientId: String) = ConsumerConfig(ConfigFactory.parseString(
      s"""
         |kafka {
         |  consumer {
         |    name = "$consumerName"
         |    initialDelay = "500 milliseconds"
         |    interval = "500 milliseconds"
         |    pollingTimeoutMs = $pollingTimeout
         |    askTimeout = "5 seconds"
         |    properties {
         |      group.id = "group.id"
         |    }
         |  }
         |}
       """.stripMargin), topic, clientId)
  }

}
