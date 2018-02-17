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

package com.ovoenergy.kafka.serialization.json4s

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait Json4sSerialization {

  def json4sSerializer[T <: AnyRef](implicit jsonFormats: Formats): KafkaSerializer[T] = serializer { (_, data) =>
    val bout = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(bout, StandardCharsets.UTF_8)

    // TODO Use scala-arm
    try {
      write(data, writer)
      writer.flush()
    } finally {
      writer.close()
    }
    bout.toByteArray
  }

  def json4sDeserializer[T: TypeTag](implicit jsonFormats: Formats): KafkaDeserializer[T] = deserializer { (_, data) =>
    val tt = implicitly[TypeTag[T]]
    implicit val cl = ClassTag[T](tt.mirror.runtimeClass(tt.tpe))
    read[T](new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))
  }

}
