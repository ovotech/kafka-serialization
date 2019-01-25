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

package com.ovoenergy.kafka.serialization
package avro4s2

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.ByteBuffer

import com.github.tomakehurst.wiremock.client.WireMock._
import com.ovoenergy.kafka.serialization.testkit.UnitSpec._
import com.ovoenergy.kafka.serialization.testkit.{UnitSpec, WireMockFixture}
import org.apache.avro.Schema

import com.sksamuel.avro4s._

class Avro4s2SerializationSpec extends UnitSpec with WireMockFixture {

  "Avro4sSerialization" when {
    "serializing binary" when {
      "is value serializer" should {
        "register the schema to the schemaRegistry for value" in forAll { (topic: String, event: Event) =>
          testWithPostSchemaExpected(s"$topic-value") {
            val serializer = avroBinarySchemaIdSerializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
            serializer.serialize(topic, event)
          }
        }

        "register the schema to the schemaRegistry for value only once" in forAll {
          (topic: String, events: List[Event]) =>
            whenever(events.length > 1) {
              // This verifies that the HTTP cal to the schema registry happen only once.
              testWithPostSchemaExpected(s"$topic-value") {
                val serializer =
                  avroBinarySchemaIdSerializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
                events.foreach(event => serializer.serialize(topic, event))
              }
            }
        }

      }

      "is key serializer" should {
        "register the schema to the schemaRegistry for key" in forAll { (topic: String, event: Event) =>
          testWithPostSchemaExpected(s"$topic-key") {
            val serializer = avroBinarySchemaIdSerializer[Event](wireMockEndpoint, isKey = true, includesFormatByte = false)
            serializer.serialize(topic, event)
          }
        }
      }
    }

    "serializing json" when {
      "the value is serializer" should {
        "register the schema to the schemaRegistry for value" in forAll { (topic: String, event: Event) =>
          testWithPostSchemaExpected(s"$topic-value") {
            val serializer = avroJsonSchemaIdSerializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
            serializer.serialize(topic, event)
          }
        }

        "register the schema to the schemaRegistry for value only once" in forAll {
          (topic: String, events: List[Event]) =>
            whenever(events.length > 1) {
              // This verifies that the HTTP cal to the schema registry happen only once.
              testWithPostSchemaExpected(s"$topic-value") {
                val serializer = avroJsonSchemaIdSerializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
                events.foreach(event => serializer.serialize(topic, event))
              }
            }
        }
      }

      "is key serializer" should {
        "register the schema to the schemaRegistry for key" in forAll { (topic: String, event: Event) =>
          testWithPostSchemaExpected(s"$topic-key") {
            val serializer = avroJsonSchemaIdSerializer[Event](wireMockEndpoint, isKey = true, includesFormatByte = false)
            serializer.serialize(topic, event)
          }
        }
      }
    }

    "deserializing binary" when {
      "the writer schema is used" should {
        "read the schema from the registry" in forAll { (topic: String, schemaId: Int, event: Event) =>
          val deserializer = avroBinarySchemaIdDeserializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)

          val bytes = asAvroBinaryWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, SchemaFor[Event].schema)

          val deserialized = deserializer.deserialize(topic, bytes)
          deserialized shouldBe event
        }

        "read the schema from the registry only once" in forAll { (topic: String, schemaId: Int, event: Event) =>
          // The looping nature of scalacheck causes to reuse the same wiremock configuration
          resetWireMock()

          val deserializer = avroBinarySchemaIdDeserializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
          val bytes = asAvroBinaryWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, SchemaFor[Event].schema)

          deserializer.deserialize(topic, bytes)
          deserializer.deserialize(topic, bytes)

          verify(1, getRequestedFor(urlMatching(s"/schemas/ids/$schemaId")))
        }

        "handle a format byte in the header" in forAll { (topic: String, schemaId: Int, event: Event) =>
          val deserializer = avroBinarySchemaIdDeserializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = true)

          val bytes = Array(0: Byte) ++ asAvroBinaryWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, SchemaFor[Event].schema)

          val deserialized = deserializer.deserialize(topic, bytes)
          deserialized shouldBe event
        }
      }
    }

    "deserializing json" when {
      "the writer schema is used" should {
        "read the schema from the registry" in forAll { (topic: String, schemaId: Int, event: Event) =>
          val deserializer = avroJsonSchemaIdDeserializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
          val bytes = asAvroJsonWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, SchemaFor[Event].schema)

          val deserialized = deserializer.deserialize(topic, bytes)

          deserialized shouldBe event
        }

        "read the schema from the registry only once" in forAll { (topic: String, schemaId: Int, event: Event) =>
          // The looping nature of scalacheck causes to reuse the same wiremock configuration
          resetWireMock()

          val deserializer = avroJsonSchemaIdDeserializer[Event](wireMockEndpoint, isKey = false, includesFormatByte = false)
          val bytes = asAvroJsonWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, SchemaFor[Event].schema)

          deserializer.deserialize(topic, bytes)
          deserializer.deserialize(topic, bytes)

          verify(1, getRequestedFor(urlMatching(s"/schemas/ids/$schemaId")))
        }
      }
    }
  }

  private def givenSchema(schemaId: Int, schema: Schema) = {

    // The schema property is a string containing JSON.
    val schemaBody = "{\"schema\": \"" + schema.toString.replace(""""""", """\"""") + "\"}"

    stubFor(
      get(urlMatching(s"/schemas/ids/$schemaId"))
        .willReturn(
          aResponse()
            .withBody(schemaBody)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )
  }

  private def asAvroBinaryWithSchemaIdBytes[T: Encoder](t: T, schemaId: Int)(implicit schemaFor: SchemaFor[T]): Array[Byte] =
    asAvroWithSchemaIdBytes(t, schemaId, out => AvroOutputStream.binary[T].to(out).build(schemaFor.schema))

  private def asAvroJsonWithSchemaIdBytes[T: SchemaFor: Encoder](t: T, schemaId: Int)(implicit schemaFor: SchemaFor[T]): Array[Byte] =
    asAvroWithSchemaIdBytes(t, schemaId, out => AvroOutputStream.json[T].to(out).build(schemaFor.schema))

  private def asAvroWithSchemaIdBytes[T](
    t: T,
    schemaId: Int,
    mkAvroOut: OutputStream => AvroOutputStream[T]
  ): Array[Byte] = {
    val bout = new ByteArrayOutputStream()
    val avroOut = mkAvroOut(bout)
    avroOut.write(t)
    avroOut.flush()
    ByteBuffer.allocate(bout.size() + 4).putInt(schemaId).put(bout.toByteArray).array()
  }

  private def testWithPostSchemaExpected[T](subject: String)(f: => T) = {

    stubFor(
      post(urlMatching("/subjects/.*/versions"))
        .willReturn(
          aResponse()
            .withBody("{\"id\": 1}")
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

    val result = f

    // TODO verify the schema is the same
    verify(postRequestedFor(urlEqualTo(s"/subjects/$subject/versions")))

    result
  }
}
