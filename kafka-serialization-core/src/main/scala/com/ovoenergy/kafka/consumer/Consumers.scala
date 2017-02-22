package com.ovoenergy.kafka.consumer

import java.util

import akka.event.LoggingAdapter
import com.ovoenergy.kafka.model.Event._
import com.ovoenergy.kafka.consumer.KafkaConsumerClient.Protocol
import com.ovoenergy.kafka.consumer.KafkaConsumerClient.Protocol.Subscribe
import com.ovoenergy.kafka.model.Event
import org.apache.kafka.clients.consumer.{Consumer, ConsumerRecords, OffsetAndMetadata, OffsetCommitCallback}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[consumer] trait Consumers {

  /**
    * Consume records asynchronously by feeding them to subscribers.
    *
    * @param records     records to consume
    * @param subscribers subscribers to feed
    */
  def feedSubscribers(records: ConsumerRecords[Key, Envelope], subscribers: Seq[Subscribe])(implicit ec: ExecutionContext): Future[Protocol.Done.type] = Future.sequence {
    for {
      record <- records
      event = Event(record.topic(), Message(record.key(), record.value()))
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

}

private[consumer] object Consumers extends Consumers {

  /**
    * Class to augment the Apache Kafka Consumer.
    */
  implicit class RichConsumer[K, V](val consumer: Consumer[K, V]) extends AnyVal {

    /**
      * Commit offsets returned on the last poll() for the subscribed list of topics and partitions.
      *
      * @return failed future if commit failed.
      */
    def asyncCommit(f: (Map[TopicPartition, OffsetAndMetadata], Exception) => Unit): Future[Protocol.Done.type] = {
      val promise = Promise[Protocol.Done.type]
      consumer.commitAsync(new OffsetCommitCallback {
        override def onComplete(offsets: util.Map[TopicPartition, OffsetAndMetadata], exception: Exception): Unit = {
          Try(f(offsets.asScala.toMap, exception)) match {
            case Success(_) => promise.trySuccess(Protocol.Done)
            case Failure(thrown) => promise.tryFailure(thrown)
          }
        }
      })
      promise.future
    }

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
