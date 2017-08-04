package com.ovoenergy.kafka.serialization.core

import java.util

import com.ovoenergy.kafka.serialization.core.syntax._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer}

/**
  * Provides all the basic function to build a Kafka Deserializer.
  */
private[core] trait Deserialization {

  /**
    * Builds a Kafka deserializer from a function `(String, Array[Byte]) => T` and a close function.
    *
    * The close function is useful for those serializer that need to allocate some resources.
    */
  def deserializer[T](f: (String, Array[Byte]) => T, c: () => Unit): KafkaDeserializer[T] = new KafkaDeserializer[T] {

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = c()

    override def deserialize(topic: String, data: Array[Byte]): T = f(topic, data)
  }

  /**
    * Builds a Kafka deserializer from a function `(String, Array[Byte]) => T`.
    */
  def deserializer[T](f: (String, Array[Byte]) => T): KafkaDeserializer[T] = deserializer(f, () => Unit)

  /**
    * Builds a Kafka deserializer from a function `String => T`.ignoring the topic as it is usually not taken in
    * account.
    */
  def deserializer[T](f: Array[Byte] => T): KafkaDeserializer[T] = deserializer { (_, bytes) =>
    f(bytes)
  }

  /**
    * Wraps a Kafka deserializer by dropping the first byte of the payload (assuming it is the format byte).
    */
  def formatDroppingDeserializer[T](d: KafkaDeserializer[T]): KafkaDeserializer[T] =
    deserializer({ (topic, data) =>
      d.deserialize(topic, data.drop(1))
    })

  /**
    * Wraps a Kafka deserializer by checking if the first byte is the desired format byte.
    *
    * By default the format byte will be also dropped. This behaviour can be controlled by the `dropFormat` parmaeter.
    */
  def formatCheckingDeserializer[T](expectedFormat: Format,
                                    d: KafkaDeserializer[T],
                                    dropFormat: Boolean = true): KafkaDeserializer[T] =
    deserializer({ (topic, data) =>
      (if (data.isEmpty) {
         nullDeserializer[T]
       } else if (data(0) == Format.toByte(expectedFormat) && dropFormat) {
         formatDroppingDeserializer(d)
       } else if (data(0) == Format.toByte(expectedFormat) && !dropFormat) {
         d
       } else {
         failingDeserializer(new UnsupportedFormatException(data(0).toFormat))
       }).deserialize(topic, data)
    })

  /**
    * This allow to build a Kafka deserializer that will be able to deserialize payloads encoded in different way.
    *
    * If the payload format does not match any [[Format]] handled by the `Format => serializer` partial function the
    * given default Kafka deserializer will be used to deserialize the payload.
    *
    * The `dropFormat` parameter controls whether passing the whole payload to the following deserializer or dropping it.
    * By default the format byte is dropped.
    */
  def formatDemultiplexerDeserializer[T](unknownFormat: Format => KafkaDeserializer[T], dropFormat: Boolean = true)(
    pf: PartialFunction[Format, KafkaDeserializer[T]]
  ): KafkaDeserializer[T] =
    deserializer({ (topic, data) =>
      pf.andThen(d => if (dropFormat) formatDroppingDeserializer(d) else d)
        .applyOrElse(Format.fromByte(data(0)), unknownFormat)
        .deserialize(topic, data)
    })

  /**
    * This allow to build a Kafka deserializer that will be able to apply different Kafka deserializer depending
    * on the source topic.
    *
    * If the source topic does not match any topic handled by the `String => deserializer` partial function the given
    * default Kafka deserializer will be used to deserialize the payload.
    */
  def topicDemultiplexerDeserializer[T](
    noMatchingTopic: String => KafkaDeserializer[T]
  )(pf: PartialFunction[String, KafkaDeserializer[T]]): KafkaDeserializer[T] =
    deserializer({ (topic, data) =>
      pf.applyOrElse[String, KafkaDeserializer[T]](topic, noMatchingTopic).deserialize(topic, data)
    })

  /**
    * Wraps a Kafka deserializer to make it non-strict. The deserialization logic will be applied lazily.
    */
  def nonStrictDeserializer[T](d: KafkaDeserializer[T]): KafkaDeserializer[() => T] =
    deserializer({ (topic, data) => () =>
      d.deserialize(topic, data)
    })

  /**
    * Builds a Kafka deserializer that will return the payload as-is.
    */
  def identityDeserializer: KafkaDeserializer[Array[Byte]] = identity[Array[Byte]] _

  /**
    * Builds a Kafka deserializer that will return always the same value.
    */
  def constDeserializer[T](t: T): KafkaDeserializer[T] = deserializer(_ => t)

  /**
    * Builds a Kafka deserializer that will return always null.
    */
  def nullDeserializer[T]: KafkaDeserializer[T] = constDeserializer(null.asInstanceOf[T])

  /**
    * Builds a Kafka deserializer that will always fail with the given exception.
    */
  def failingDeserializer[T](e: Throwable): KafkaDeserializer[T] = deserializer(_ => throw e)

}
