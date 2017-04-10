package com.ovoenergy.serialization.kafka.client.producer

import akka.event.LoggingAdapter
import org.apache.kafka.clients.producer.Producer

import scala.util.Try
import scala.util.control.NonFatal

private[kafka] object Producers {

  /**
    * Class to augment the Apache Kafka Producer.
    */
  implicit class RichProducer[K, V](val producer: Producer[K, V]) extends AnyVal {

    /**
      * Close the producer quietly.
      *
      * Ignores any non fatal exceptions encountered.
      */
    def closeQuietly(implicit log: LoggingAdapter): Unit = {
      log.debug(s"Closing kafka producer...")
      Try(producer.close()).recover {
        case NonFatal(thrown) => log.error(thrown, "Closing kafka producer failed!")
      }
    }

  }

}
