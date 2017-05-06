package com.ovoenergy.kafka.serialization.core

import java.util

import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

trait Serialization {

  def serializer[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = serializer(f, () => Unit)

  def serializer[T](f: (String, T) => Array[Byte], c: () => Unit): KafkaSerializer[T] = new KafkaSerializer[T] {

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = c()

    override def serialize(topic: String, data: T): Array[Byte] = f(topic, data)
  }

  def serializer[T](f: T => Array[Byte]): KafkaSerializer[T] = serializer { (_, t) =>
    f(t)
  }

  def formatSerializer[T](magicByte: Format, delegate: KafkaSerializer[T]): KafkaSerializer[T] = serializer({ (topic, data) =>
    Array(magicByte.toByte) ++ delegate.serialize(topic, data)
  })

  def topicMultiplexerSerializer[T](noMatchingTopic: => KafkaSerializer[T])(pf: PartialFunction[String, KafkaSerializer[T]]): KafkaSerializer[T] = {
    serializer({(topic, data) =>
      pf.applyOrElse[String, KafkaSerializer[T]](topic, _ => noMatchingTopic).serialize(topic, data)
    })
  }

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

  def formatCheckingDeserializer[T](expectedFormat: Format, d: KafkaDeserializer[T]): KafkaDeserializer[T] = deserializer({ (topic, data) =>
    if (data.isEmpty) {
      // Kafka API requirements :(
      null.asInstanceOf[T]
    } else if (data(0) == Format.toByte(expectedFormat)) {
      d.deserialize(topic, data.drop(1))
    } else {
      d.deserialize(topic, data)
    }
  })

  def formatDemultiplexerDeserializer[T](noByteFormatter: KafkaDeserializer[T])(pf: PartialFunction[Format, KafkaDeserializer[T]]): KafkaDeserializer[T] = {
    deserializer({ (topic, data) =>
      Format.fromByte(data(0)).map { format =>
        if (pf.isDefinedAt(format)) {
          pf.apply(format)
        } else {
          noByteFormatter
        }
      }.getOrElse(noByteFormatter).deserialize(topic, data)
    })
  }

  def topicDemultiplexerDeserializer[T](noMatchingTopic: => KafkaDeserializer[T])(pf: PartialFunction[String, KafkaDeserializer[T]]): KafkaDeserializer[T] = {
    deserializer({ (topic, data) =>
      pf.applyOrElse[String, KafkaDeserializer[T]](topic, _ => noMatchingTopic).deserialize(topic, data)
    })
  }

  def nonStrictDeserializer[T](d: KafkaDeserializer[T]): KafkaDeserializer[() => T] = deserializer({ (topic, data) =>
    () => d.deserialize(topic, data)
  })

}
