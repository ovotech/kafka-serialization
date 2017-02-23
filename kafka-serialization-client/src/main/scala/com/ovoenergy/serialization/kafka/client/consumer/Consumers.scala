package com.ovoenergy.serialization.kafka.client.consumer

import akka.event.LoggingAdapter
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Protocol
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumerClient.Protocol.Subscribe
import com.ovoenergy.serialization.kafka.client.model.Event
import com.ovoenergy.serialization.kafka.client.model.Event._
import org.apache.kafka.clients.consumer._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[consumer] trait Consumers {

  /**
    * Consume records asynchronously by feeding them to subscribers.
    *
    * @param records     records to consume
    * @param subscribers subscribers to feed
    */
  def feedSubscribers(records: ConsumerRecords[Try[Key], Try[Envelope]], subscribers: Seq[Subscribe])(implicit ec: ExecutionContext): Future[Protocol.Done.type] = Future.sequence {
    for {
      record <- records
      event = recordToEvent(record)
      subscriber <- subscribers if subscriber.value.isDefinedAt(event)
    } yield {
      Try(subscriber.value(event)) match {
        case Success(value) => value
        case Failure(thrown) => Future.failed(thrown)
      }
    }
  } map { _ =>
    Protocol.Done
  }

  private def recordToEvent(record: ConsumerRecord[Try[Key], Try[Envelope]]): Try[Event] = (record.key, record.value) match {
    case (Success(k), Success(v)) => Success(Event(record.topic(), Message(k, v)))
    case (Failure(ex), _) => Failure(ex)
    case (_, Failure(ex)) => Failure(ex)
  }

}

private[consumer] object Consumers extends Consumers {

  /**
    * Class to augment the Apache Kafka Consumer.
    */
  implicit class RichConsumer[K, V](val consumer: Consumer[K, V]) extends AnyVal {

    /**
      * Close the consumer quietly.
      *
      * Ignores any non fatal exceptions encountered.
      */
    def closeQuietly(implicit log: LoggingAdapter): Unit = {
      log.debug(s"Closing kafka consumer...")
      Try(consumer.close()).recover {
        case NonFatal(thrown) => log.error(thrown, "Closing kafka consumer failed!")
      }
    }

  }

}
