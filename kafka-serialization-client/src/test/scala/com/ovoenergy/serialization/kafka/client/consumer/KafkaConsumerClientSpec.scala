package com.ovoenergy.serialization.kafka.client.consumer

import java.util

import com.ovoenergy.serialization.kafka.client.{ActorSpecContext, Topic}
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Subscriber
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.collection.JavaConversions._
import scala.concurrent.Future

class KafkaConsumerClientSpec extends Specification with Mockito with ScalaFutures with PatienceConfiguration {

  "KafkaConsumerClientSpec" should {

    "subscribe to topic" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns ConsumerRecords.empty()

      val client = consumerClient(ConsumerName("test1"), ClientId("subscribe-client-id"), Topic("subscribe-to-topic"))
      client.subscribe(success).futureValue

      eventually {
        there was one(consumer).subscribe(Seq("subscribe-to-topic"))
      }
    }

    "poll for messages" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns ConsumerRecords.empty()

      val client = consumerClient(ConsumerName("test2"), ClientId("poll-for-message-client-id"), Topic("poll-for-message-topic"))
      client.subscribe(success).futureValue

      eventually {
        there was atLeastOne(consumer).poll(pollingTimeout)
      }
    }

    "let subscribers subscribe to configured topic" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns ConsumerRecords.empty()

      val client = consumerClient(ConsumerName("test3"), ClientId("clientId"), Topic("topic"))

      client.subscribe(success).futureValue

      whenReady(client.subscriber) { subscriber =>
        subscriber should not be empty
      }

    }

    "recycle existing subscriber if restarted" in new KafkaConsumerClientSpecContext {
      consumer.poll(pollingTimeout) returns records(record("recycle-topic-1"))

      val client = consumerClient(ConsumerName("test4"), ClientId("recycle-client-id-1"), Topic("recycle-topic-1"))

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

      val client = consumerClient(ConsumerName("test6"), ClientId("commit-client-id"), Topic("commit-topic-1"))

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

      val client = consumerClient(ConsumerName("test7"), ClientId("restart-client-id"), Topic("restart-topic-1"))

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
    val consumer = mock[Consumer[String, String]]
    val pollingTimeout = 1000L

    def record(topic: String) = new ConsumerRecord[String, String](topic, 0, 0L, "key", "value")

    def records(values: ConsumerRecord[String, String]*): ConsumerRecords[String, String] = {
      new ConsumerRecords[String, String](values.groupBy(_.topic()).map { case (topic, list) =>
        new TopicPartition(topic, 0) -> util.Arrays.asList(list: _*)
      })
    }

    val success: Subscriber[String, String] = {
      case _ => Future.successful((): Unit)
    }

    val failure: Subscriber[String, String] = {
      case _ => Future.failed(new RuntimeException)
    }

    val naughtySubscriber: Subscriber[String, String] = {
      case _ => throw new RuntimeException
    }

    def consumerClient(consumerName: ConsumerName, clientId: ClientId, topic: Topic): KafkaConsumer[String, String] = KafkaConsumer(config(consumerName, clientId, topic), () => consumer)

    def config(consumerName: ConsumerName, clientId: ClientId, topic: Topic) = ConsumerConfig(ConfigFactory.parseString(
      s"""
         |kafka {
         |  consumer {
         |    initialDelay = "500 milliseconds"
         |    interval = "500 milliseconds"
         |    pollingTimeoutMs = $pollingTimeout
         |    askTimeout = "5 seconds"
         |    properties {
         |      group.id = "group.id"
         |    }
         |  }
         |}
       """.stripMargin), consumerName, clientId, topic)
  }

}
