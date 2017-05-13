package com.ovoenergy.kafka.serialization.circe

import java.nio.charset.StandardCharsets

import cats.syntax.either._
import com.ovoenergy.kafka.serialization.core._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Error, Json}
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

private[circe] trait CirceSerialization {

  def circeJsonSerializer[T: Encoder]: KafkaSerializer[T] = serializer { (_, data) =>
    data.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
  }

  def circeJsonDeserializer[T: Decoder]: KafkaDeserializer[T] = deserializer { (_, data) =>
    (for {
      json <- parse(new String(data, StandardCharsets.UTF_8)): Either[Error, Json]
      t <- json.as[T]: Either[Error, T]
    } yield
      t).fold(error => throw new RuntimeException(s"Deserialization failure: ${error.getMessage}", error), identity _)
  }

}
