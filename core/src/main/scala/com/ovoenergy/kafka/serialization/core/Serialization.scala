package com.ovoenergy.kafka.serialization.core

import com.ovoenergy.kafka.serialization.core.syntax._
import java.util

import org.apache.kafka.common.serialization.{Serializer => KafkaSerializer}

/**
  * Provides all the basic function to build a Kafka Serializer.
  */
private[core] trait Serialization {

  /**
    * Builds a Kafka serializer from a function `(String, T) => Array[Byte]` and close function.
    *
    * The close function is useful for those serializer that need to allocate some resources.
    */
  def serializer[T](f: (String, T) => Array[Byte], c: () => Unit): KafkaSerializer[T] = new KafkaSerializer[T] {

    // Keep in mind that for serializer built with this library Kafka will never call `configure`.
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = c()

    override def serialize(topic: String, data: T): Array[Byte] = f(topic, data)
  }

  /**
    * Builds a Kafka serializer from a function `(String, T) => Array[Byte]`
    */
  def serializer[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = serializer(f, () => Unit)

  /**
    * Builds a Kafka serializer from a function `String => Array[Byte]` the topic is ignored as most of the time
    * it is not taken in account.
    */
  def serializer[T](f: T => Array[Byte]): KafkaSerializer[T] = serializer { (_, t) =>
    f(t)
  }

  /**
    * Wraps a Kafka serializer prepending the format byte to the original payload.
    */
  def formatSerializer[T](format: Format, delegate: KafkaSerializer[T]): KafkaSerializer[T] =
    serializer({ (topic, data) =>
      Array(format.toByte) ++ delegate.serialize(topic, data)
    })

  /**
    * Provides a way to use different serializer depending of the target topic.
    */
  def topicMultiplexerSerializer[T](
    noMatchingTopic: String => KafkaSerializer[T]
  )(pf: PartialFunction[String, KafkaSerializer[T]]): KafkaSerializer[T] =
    serializer({ (topic, data) =>
      pf.applyOrElse[String, KafkaSerializer[T]](topic, noMatchingTopic).serialize(topic, data)
    })

  /**
    * Builds a serializer that will serialize the message as-is.
    */
  def identitySerializer: KafkaSerializer[Array[Byte]] = identity[Array[Byte]] _

  /**
    * Builds a serializer that always serialize the same payload.
    */
  def constSerializer[T](bytes: Array[Byte]): KafkaSerializer[T] = serializer(_ => bytes)

  /**
    * Builds a serializer that always serialize null.
    */
  def nullSerializer[T]: KafkaSerializer[T] = serializer(_ => null.asInstanceOf[Array[Byte]])

  /**
    * Builds a serializer that always fails with the given exception.
    */
  def failingSerializer[T](e: Throwable): KafkaSerializer[T] = serializer(_ => throw e)

}
