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

import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import com.ovoenergy.kafka.serialization.testkit.UnitSpec._
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._

class Json4sSerializationSpec extends UnitSpec with Json4sSerialization {

  implicit val formats = DefaultFormats

  "Json4sSerialization" when {
    "serializing" should {
      "write the Json json body" in forAll { event: Event =>
        val serializer = json4sSerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        read[Event](new String(bytes, UTF_8)) shouldBe event
      }
    }

    "deserializing" should {
      "parse the json" in forAll { event: Event =>
        val deserializer = json4sDeserializer[Event]

        val bytes = write(event).getBytes(UTF_8)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
