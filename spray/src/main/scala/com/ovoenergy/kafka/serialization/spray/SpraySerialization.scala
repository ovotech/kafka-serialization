package com.ovoenergy.kafka.serialization.spray

import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core.Format.Json
import com.ovoenergy.kafka.serialization.core.Serialization
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import spray.json._

trait SpraySerialization extends Serialization {

  def spraySerializer[T](implicit format: JsonWriter[T]): KafkaSerializer[T] = formatSerializer(Json, serializer { (_, data) =>
    val bout = new ByteArrayOutputStream()
    val osw = new OutputStreamWriter(bout, StandardCharsets.UTF_8)
    try {
      osw.write(data.toJson.compactPrint)
      osw.flush()
    } finally {
      osw.close()
    }
    bout.toByteArray
  })

  def sprayDeserializer[T](implicit format: JsonReader[T]): KafkaDeserializer[T] = formatCheckingDeserializer(Json, deserializer { (_, data) =>
    JsonParser(ParserInput(data)).convertTo[T]
  })

}

