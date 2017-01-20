package com.ovoenergy.kafka.serialization

import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.Serialization._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

object CirceSerialization {

  def serializerWithCirceJson[T: Encoder]: KafkaSerializer[T] = serializerWithMagicByte(Format.Json, serializer { (_, data) =>
    data.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
  })

  def deserializerWithCirceJson[T: Decoder]: KafkaDeserializer[T] = deserializerWithFirstByteDropping(deserializer { (_, data) =>
    (for {
      json <- parse(new String(data, StandardCharsets.UTF_8))
      t <- json.as[T]
    } yield t).fold(error => throw new RuntimeException(s"Deserialization failure: ${error.getMessage}", error), identity)
  })

}
