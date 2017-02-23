package com.ovoenergy.serialization.kafka.client.producer

import com.ovoenergy.serialization.kafka.client.ActorSpecContext
import com.ovoenergy.serialization.kafka.client.model.Event
import com.ovoenergy.serialization.kafka.client.util.DurationUtils
import org.apache.kafka.clients.producer.MockProducer
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

class KafkaProducerClientSpec extends Specification with ScalaFutures with PatienceConfiguration with DurationUtils {

  trait EventProducerContext extends ActorSpecContext {
    val toSend: Event = Event.of("topic", Event.Key("TestEvent", None), Event.Envelope("test-event-1", None, "Test"))
    val mockProducer = new MockProducer[Event.Key, Event.Envelope](false, null, null)
    // todo
    val initialDelay = getFiniteDuration("500 milliseconds")
    val interval = initialDelay

    def producerClient(id: String) = KafkaProducer(ProducerConfig(initialDelay, interval, id), mockProducer)
  }

  sequential

  "EventProducerSpec" should {

    "start actor and accept events" in new EventProducerContext {
      val producer = producerClient("test-1")
      producer.produce(toSend)

      eventually {
        mockProducer.completeNext() === true
        val history = mockProducer.history().asScala
        history must not be empty
        val record = history.head
        record.key() must_== toSend.message.key
        record.value() must_== toSend.message.value
      }
    }

    "recover from failure" in new EventProducerContext {
      val producer = producerClient("test-2")
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
