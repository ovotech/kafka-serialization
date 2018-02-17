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

import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.kafka.serialization.spray.SpraySerializationSpec._
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import com.ovoenergy.kafka.serialization.testkit.UnitSpec._
import spray.json.DefaultJsonProtocol._
import spray.json._

object SpraySerializationSpec {

  val IgnoredTopic = "ignored"
  implicit val EventFormat: RootJsonFormat[Event] = jsonFormat2(Event)

}

class SpraySerializationSpec extends UnitSpec with SpraySerialization {

  "SpraySerialization" when {
    "serializing" should {
      "write the json body" in forAll { event: Event =>
        val serializer = spraySerializer[Event]

        val bytes = serializer.serialize(IgnoredTopic, event)

        new String(bytes, UTF_8).parseJson shouldBe event.toJson
      }
    }

    "deserializing" should {
      "parse the json" in forAll { event: Event =>
        val deserializer = sprayDeserializer[Event]

        val bytes = event.toJson.compactPrint.getBytes(UTF_8)

        deserializer.deserialize(IgnoredTopic, bytes) shouldBe event
      }
    }
  }

}
