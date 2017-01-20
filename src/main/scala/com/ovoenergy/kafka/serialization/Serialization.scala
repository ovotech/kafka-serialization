package com.ovoenergy.kafka.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, OutputStreamWriter}
import java.nio.charset.StandardCharsets
import java.util

import cats.syntax.either._
import cats.syntax.option._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.syntax._
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import org.json4s.Formats
import org.json4s.native.Serialization._

import scala.reflect.ClassTag
import scala.util.matching.Regex
import scala.reflect.runtime.universe._

object Serialization {

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


  object Serializer {

    import Format._

    def instance[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = new KafkaSerializer[T] {

      override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

      override def close(): Unit = {}

      override def serialize(topic: String, data: T): Array[Byte] = f(topic, data)
    }

    def serializeWithMagicByte[T](magicByte: Format, serializer: KafkaSerializer[T]): KafkaSerializer[T] = instance({ (topic, data) =>
      Array(magicByte.toByte) ++ serializer.serialize(topic, data)
    })

    def serializeWithCirceJson[T: Encoder]: KafkaSerializer[T] = serializeWithMagicByte(Format.Json, instance { (_, data) =>
      data.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
    })

    def serializeWithJson4sJson[T <: AnyRef](implicit jsonFormats: Formats): KafkaSerializer[T] = serializeWithMagicByte(Format.Json, instance { (_, data) =>
      val bout = new ByteArrayOutputStream()
      write[T, OutputStreamWriter](data, new OutputStreamWriter(bout, StandardCharsets.UTF_8))
      bout.flush()
      bout.toByteArray
    })

    def serializeWithAvroBinaryAndSchemaRegistry[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] = {
      import scala.collection.JavaConverters._

      val toRecord = implicitly[ToRecord[T]]
      val serializer = new KafkaAvroSerializer()
      serializer.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

      Serializer.instance({ (topic, t) =>
        serializer.serialize(topic, toRecord(t))
      })
    }

    def serializeWithTopicMultiplexer[T](entries: (TopicMatcher, KafkaSerializer[T])*): KafkaSerializer[T] = {
      instance({ (topic, data) =>
        entries.find {
          case (k, v) if k(topic) => true
        }.get._2.serialize(topic, data)
        // TODO catch and try serialization exception
      })
    }
  }

  object Deserializer {

    def instance[T](f: (String, Array[Byte]) => T): KafkaDeserializer[T] = new KafkaDeserializer[T] {

      override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

      override def close(): Unit = {}

      override def deserialize(topic: String, data: Array[Byte]): T = f(topic, data)
    }

    def deserializeDroppingFirstByte[T](d: KafkaDeserializer[T]): KafkaDeserializer[T] = instance({ (topic, data) =>
      d.deserialize(topic, data.drop(1))
    })

    def deserializeWithCirceJson[T: Decoder]: KafkaDeserializer[T] = deserializeDroppingFirstByte(instance { (_, data) =>
      (for {
        json <- parse(new String(data, StandardCharsets.UTF_8))
        t <- json.as[T]
      } yield t).fold(error => throw new RuntimeException(s"Deserialization failure: ${error.getMessage}", error), identity)
    })

    def deserializeWithJson4s[T: TypeTag](implicit jsonFormats: Formats): KafkaDeserializer[T] = deserializeDroppingFirstByte(instance { (_, data) =>
      val tt = implicitly[TypeTag[T]]
      implicit val cl = ClassTag[T](tt.mirror.runtimeClass(tt.tpe))
      read[T](new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))
    })

    def deserializeAvroBinaryAndSchemaRegistry[T: FromRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
      import scala.collection.JavaConverters._

      val fromRecord = implicitly[FromRecord[T]]

      val d: KafkaAvroDeserializer = new KafkaAvroDeserializer
      d.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

      instance({ (topic, data) =>
        fromRecord(d.deserialize(topic, data).asInstanceOf[GenericRecord])
      })
    }

    def deserializeAvroBinaryAndSchemaRegistry[T: FromRecord : SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
      import scala.collection.JavaConverters._

      val fromRecord = implicitly[FromRecord[T]]
      val schemaFor = implicitly[SchemaFor[T]]

      val d: KafkaAvroDeserializer = new KafkaAvroDeserializer
      d.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

      instance({ (topic, data) =>
        fromRecord(d.deserialize(topic, data, schemaFor()).asInstanceOf[GenericRecord])
      })
    }

    def deserializeWithMagicByteDemultiplexer[T](entries: (Byte, KafkaDeserializer[T])*): KafkaDeserializer[T] = {
      val entriesAsMap: Map[Byte, KafkaDeserializer[T]] = entries.toMap

      instance({ (topic, data) =>
        entriesAsMap(data(0)).deserialize(topic, data)
      })
    }

    def deserializeWithTopicDemultiplexer[T](entries: (TopicMatcher, KafkaDeserializer[T])*): KafkaDeserializer[T] = {
      instance({ (topic, data) =>
        entries.find {
          case (k, v) if k(topic) => true
        }.get._2.deserialize(topic, data)
        // TODO catch and try deserialization exception
      })
    }

    def deserializeNonStrict[T](d: KafkaDeserializer[T]): KafkaDeserializer[() => T] = instance({ (topic, data) =>
      () => d.deserialize(topic, data)
    })
  }

}

