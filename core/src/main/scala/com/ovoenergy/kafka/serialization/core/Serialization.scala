package com.ovoenergy.kafka.serialization.core

import  com.ovoenergy.kafka.serialization.core.syntax._
import java.util

import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

private[core] trait Serialization {

  def serializer[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = serializer(f, () => Unit)

  def serializer[T](f: (String, T) => Array[Byte], c: () => Unit): KafkaSerializer[T] = new KafkaSerializer[T] {

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = c()

    override def serialize(topic: String, data: T): Array[Byte] = f(topic, data)
  }

  def serializer[T](f: T => Array[Byte]): KafkaSerializer[T] = serializer { (_, t) =>
    f(t)
  }

  def formatSerializer[T](format: Format, delegate: KafkaSerializer[T]): KafkaSerializer[T] = serializer({ (topic, data) =>
    Array(format.toByte) ++ delegate.serialize(topic, data)
  })

  def topicMultiplexerSerializer[T](noMatchingTopic: String => KafkaSerializer[T])(pf: PartialFunction[String, KafkaSerializer[T]]): KafkaSerializer[T] = {
    serializer({(topic, data) =>
      pf.applyOrElse[String, KafkaSerializer[T]](topic, noMatchingTopic).serialize(topic, data)
    })
  }

  def identitySerializer: KafkaSerializer[Array[Byte]] = identity[Array[Byte]] _

  def constSerializer[T](bytes: Array[Byte]): KafkaSerializer[T] = serializer(_ => bytes)

  def nullSerializer[T]: KafkaSerializer[T] = serializer(_ => null.asInstanceOf[Array[Byte]])

  def failingSerializer[T](e: Throwable): KafkaSerializer[T] = serializer(_ => throw e)

  def deserializer[T](f: (String, Array[Byte]) => T): KafkaDeserializer[T] = deserializer(f, () => Unit)

  def deserializer[T](f: (String, Array[Byte]) => T, c: () => Unit): KafkaDeserializer[T] = new KafkaDeserializer[T] {

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = c()

    override def deserialize(topic: String, data: Array[Byte]): T = f(topic, data)
  }

  def deserializer[T](f: Array[Byte] => T): KafkaDeserializer[T] = deserializer { (_, bytes) =>
    f(bytes)
  }

  def formatDroppingDeserializer[T](d: KafkaDeserializer[T]): KafkaDeserializer[T] = deserializer({ (topic, data) =>
    d.deserialize(topic, data.drop(1))
  })

  def formatCheckingDeserializer[T](expectedFormat: Format, d: KafkaDeserializer[T], dropFormat: Boolean = true): KafkaDeserializer[T] = deserializer({ (topic, data) =>
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

  def formatDemultiplexerDeserializer[T](unknownFormat: Format => KafkaDeserializer[T], dropFormat: Boolean = true)(pf: PartialFunction[Format, KafkaDeserializer[T]]): KafkaDeserializer[T] = {
    deserializer({ (topic, data) =>
      val d = pf.applyOrElse[Format, KafkaDeserializer[T]](Format.fromByte(data(0)), unknownFormat)
      (if(dropFormat) {
        formatDroppingDeserializer(d)
      } else {
        d
      }).deserialize(topic, data)
    })
  }

  def topicDemultiplexerDeserializer[T](noMatchingTopic: String => KafkaDeserializer[T])(pf: PartialFunction[String, KafkaDeserializer[T]]): KafkaDeserializer[T] = {
    deserializer({ (topic, data) =>
      pf.applyOrElse[String, KafkaDeserializer[T]](topic, noMatchingTopic).deserialize(topic, data)
    })
  }

  def nonStrictDeserializer[T](d: KafkaDeserializer[T]): KafkaDeserializer[() => T] = deserializer({ (topic, data) =>
    () => d.deserialize(topic, data)
  })

  def identityDeserializer: KafkaDeserializer[Array[Byte]] = identity[Array[Byte]] _

  def constDeserializer[T](t: T): KafkaDeserializer[T] = deserializer(_ => t)

  def nullDeserializer[T]: KafkaDeserializer[T] = deserializer(_ => null.asInstanceOf[T])

  def failingDeserializer[T](e: Throwable): KafkaDeserializer[T] = deserializer(_ => throw e)

}
