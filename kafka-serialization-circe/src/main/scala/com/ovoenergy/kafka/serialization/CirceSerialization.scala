package com.ovoenergy.kafka.serialization

import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.Serialization._
import cats.syntax.either._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Error, Json}
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

object CirceSerialization {

  def circeJsonSerializer[T: Encoder]: KafkaSerializer[T] = formatSerializer(Format.Json, serializer { (_, data) =>
    data.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
  })

  def circeJsonDeserializer[T: Decoder]: KafkaDeserializer[T] = formatCheckingDeserializer(Format.Json, deserializer { (_, data) =>
    (for {
      json <- parse(new String(data, StandardCharsets.UTF_8)): Either[Error, Json]
      t <- json.as[T]: Either[Error, T]
    } yield t).fold(error => throw new RuntimeException(s"Deserialization failure: ${error.getMessage}", error), identity _)
  })

}
