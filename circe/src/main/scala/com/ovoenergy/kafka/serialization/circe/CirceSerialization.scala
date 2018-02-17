/*
 * Copyright 2017 OVO Energy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
