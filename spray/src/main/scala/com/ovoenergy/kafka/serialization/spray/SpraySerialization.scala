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

package com.ovoenergy.kafka.serialization.spray

import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import spray.json._
import com.ovoenergy.kafka.serialization.core._

trait SpraySerialization {

  def spraySerializer[T](implicit format: JsonWriter[T]): KafkaSerializer[T] = serializer { (_, data) =>
    val bout = new ByteArrayOutputStream()
    val osw = new OutputStreamWriter(bout, StandardCharsets.UTF_8)

    // TODO use scala-arm
    try {
      osw.write(data.toJson.compactPrint)
      osw.flush()
    } finally {
      osw.close()
    }
    bout.toByteArray
  }

  def sprayDeserializer[T](implicit format: JsonReader[T]): KafkaDeserializer[T] = deserializer { (_, data) =>
    JsonParser(ParserInput(data)).convertTo[T]
  }

}
