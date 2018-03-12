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

package com.ovoenergy.kafka.serialization.core

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.kafka.serialization.core.syntax._
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import scala.util.Random

import SerializationSpec._

object SerializationSpec {

  val IgnoredTopic = "Does not matter"

  val StringTopic = "string-topic"

  val IntTopic = "int-topic"

  val StringSerializer: Serializer[String] = (s: String) => s.getBytes(UTF_8)
  val StringDeserializer: Deserializer[String] = (data: Array[Byte]) => new String(data, UTF_8)

  val IntSerializer: Serializer[Int] = { (i: Int) =>
    ByteBuffer.allocate(4).putInt(i).array()
  }

  val IntDeserializer: Deserializer[Int] = { (data: Array[Byte]) =>
    ByteBuffer.wrap(data).getInt
  }
}

class SerializationSpec extends UnitSpec {

  "Serialization" when {

    "add the magic byte to the serialized data" in {
      formatSerializer(Format.Json, StringSerializer).serialize("test", "Test")(0) should be(
        Format.toByte(Format.Json)
      )
    }

    "multiplexing the topic" when {
      "the topic matches a branch" should {

        "use the matched serializer" in {

          // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
          val serializer = topicMultiplexerSerializer[Any](_ => constSerializer("foo".getBytes(UTF_8))) {
            case StringTopic => StringSerializer.asInstanceOf[Serializer[Any]]
            case IntTopic    => IntSerializer.asInstanceOf[Serializer[Any]]
          }

          val serialized = serializer.serialize(IntTopic, 56)

          serialized.deep shouldBe IntSerializer.serialize("Does not Matter", 56).deep
        }
      }

      "the topic does not match any branch" should {

        "use the non matched serializer" in {

          val expectedValue = "foo".getBytes(UTF_8)

          // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
          val serializer = topicMultiplexerSerializer[Any](_ => constSerializer(expectedValue)) {
            case StringTopic => StringSerializer.asInstanceOf[Serializer[Any]]
            case IntTopic    => IntSerializer.asInstanceOf[Serializer[Any]]
          }

          val serialized = serializer.serialize(IgnoredTopic, 56)

          serialized.deep shouldBe expectedValue.deep
        }
      }
    }

  }

}
