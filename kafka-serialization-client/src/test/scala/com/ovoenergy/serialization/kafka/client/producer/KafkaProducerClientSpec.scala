package com.ovoenergy.serialization.kafka.client.producer

import com.ovoenergy.serialization.kafka.client.ActorSpecContext
import com.ovoenergy.serialization.kafka.client.Topic
import com.ovoenergy.serialization.kafka.client.util.DurationUtils
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

class KafkaProducerClientSpec extends Specification with ScalaFutures with PatienceConfiguration with DurationUtils {

  trait EventProducerContext extends ActorSpecContext {
    val toSend = Event(Topic("topic"), "key", "vale")
    val mockProducer = new MockProducer[String, String](false, new StringSerializer, new StringSerializer)
    val initialDelay = getFiniteDuration("500 milliseconds")
    val interval = initialDelay

    def producerClient(producerName: ProducerName) = KafkaProducer(ProducerConfig(initialDelay, interval, producerName), mockProducer)
  }

  sequential

  "EventProducerSpec" should {

    "start actor and accept events" in new EventProducerContext {
      val producer = producerClient(ProducerName("test-1"))
      producer.produce(toSend)

      eventually {
        mockProducer.completeNext() === true
        val history = mockProducer.history().asScala
        history must not be empty
        val record = history.head
        record.key() must_== toSend.key
        record.value() must_== toSend.value
      }
    }

    "recover from failure" in new EventProducerContext {
      val producer = producerClient(ProducerName("test-2"))
      producer.produce(toSend)

      eventually {
        mockProducer.errorNext(new RuntimeException("???")) === true
      }
      eventually {
        mockProducer.completeNext() === true
      }
      eventually {
        mockProducer.history().asScala.size must beGreaterThanOrEqualTo(2)
      }
    }

  }

}
