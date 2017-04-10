package com.ovoenergy.serialization.kafka.spray

import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.ovoenergy.serialization.kafka.core.Serialization._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import spray.json._

trait SpraySerialization {

  def serialize[T](data: T)(implicit writer: JsonWriter[T]): Array[Byte] = {
    val bout = new ByteArrayOutputStream()
    val osw = new OutputStreamWriter(bout, StandardCharsets.UTF_8)
    try {
      osw.write(data.toJson.compactPrint)
      osw.flush()
    } finally {
      osw.close()
    }
    bout.toByteArray
  }

  def deserialize[T](data: Array[Byte])(implicit read: JsonReader[T]): T = {
    JsonParser(ParserInput(data)).convertTo[T]
  }

}

object SpraySerialization extends SpraySerialization {

  def spraySerializer[T](implicit format: JsonWriter[T]): KafkaSerializer[T] = formatSerializer(Format.Json, serializer { (_, data) =>
    serialize(data)
  })

  def sprayDeserializer[T](implicit format: JsonReader[T]): KafkaDeserializer[T] = formatCheckingDeserializer(Format.Json, deserializer { (_, data) =>
    deserialize(data)
  })

}
