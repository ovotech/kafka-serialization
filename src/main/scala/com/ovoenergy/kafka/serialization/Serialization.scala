package com.ovoenergy.kafka.serialization

import java.util

import cats.syntax.option._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

import scala.language.implicitConversions
import scala.util.matching.Regex

object Serialization {

  object Implicits {
    implicit def function2Serializer[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = serializer(f)
    implicit def function2Serializer[T](f: T => Array[Byte]): KafkaSerializer[T] = serializer(f)
    implicit def function2Deserializer[T](f: (String, Array[Byte]) => T): KafkaDeserializer[T] = deserializer(f)
    implicit def function2Deserializer[T](f: Array[Byte] => T): KafkaDeserializer[T] = deserializer(f)
  }

  sealed trait Format

  object Format {

    case object AvroBinaryWithSchema extends Format

    case object Json extends Format

    def toByte(f: Format): Byte = f match {
      case AvroBinaryWithSchema => 0
      case Json => 1
    }

    def fromByte(b: Byte): Option[Format] = b match {
      case 0 => AvroBinaryWithSchema.some
      case 1 => Json.some
      case _ => None
    }

    implicit class RichFormat(val f: Format) extends AnyVal {
      def toByte: Byte = Format.toByte(f)
    }

  }

  type TopicMatcher = String => Boolean

  object TopicMatcher {

    def startsWith(prefix: String): TopicMatcher = {
      _.startsWith(prefix)
    }

    def endsWith(suffix: String): TopicMatcher = {
      _.endsWith(suffix)
    }

    def contains(segment: String): TopicMatcher = {
      _.contains(segment)
    }

    def matches(regex: Regex): TopicMatcher = {
      case regex() => true
    }
  }

  def serializer[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = new KafkaSerializer[T] {

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = {}

    override def serialize(topic: String, data: T): Array[Byte] = f(topic, data)
  }

  def serializer[T](f: T => Array[Byte]): KafkaSerializer[T] = serializer {(_, t) =>
    f(t)
  }

  def serializerWithMagicByte[T](magicByte: Format, delegate: KafkaSerializer[T]): KafkaSerializer[T] = serializer({ (topic, data) =>
    Array(magicByte.toByte) ++ delegate.serialize(topic, data)
  })

  def serializerWithTopicMultiplexer[T](entries: (TopicMatcher, KafkaSerializer[T])*): KafkaSerializer[T] = {
    serializer({ (topic, data) =>
      entries.find {
        case (k, v) if k(topic) => true
      }.get._2.serialize(topic, data)
      // TODO catch and trow serialization exception
    })
  }

  def deserializer[T](f: (String, Array[Byte]) => T): KafkaDeserializer[T] = new KafkaDeserializer[T] {

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = {}

    override def deserialize(topic: String, data: Array[Byte]): T = f(topic, data)
  }

  def deserializer[T](f: Array[Byte] => T): KafkaDeserializer[T] = deserializer { (_, bytes) =>
    f(bytes)
  }

  def deserializerWithFirstByteDropping[T](d: KafkaDeserializer[T]): KafkaDeserializer[T] = deserializer({ (topic, data) =>
    d.deserialize(topic, data.drop(1))
  })

  def deserializerWithMagicByteDemultiplexer[T](entries: (Byte, KafkaDeserializer[T])*): KafkaDeserializer[T] = {
    val entriesAsMap: Map[Byte, KafkaDeserializer[T]] = entries.toMap

    deserializer({ (topic, data) =>
      entriesAsMap(data(0)).deserialize(topic, data)
    })
  }

  def deserializerWithTopicDemultiplexer[T](entries: (TopicMatcher, KafkaDeserializer[T])*): KafkaDeserializer[T] = {
    deserializer({ (topic, data) =>
      entries.find {
        case (k, v) if k(topic) => true
      }.get._2.deserialize(topic, data)
      // TODO catch and trow deserialization exception
    })
  }

  def nonStrictDeserializer[T](d: KafkaDeserializer[T]): KafkaDeserializer[() => T] = deserializer({ (topic, data) =>
    () => d.deserialize(topic, data)
  })


}

