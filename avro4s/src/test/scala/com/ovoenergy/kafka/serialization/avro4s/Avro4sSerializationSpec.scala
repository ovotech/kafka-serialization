package com.ovoenergy.kafka.serialization.avro4s

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.ByteBuffer

import com.github.tomakehurst.wiremock.client.WireMock
import com.ovoenergy.kafka.serialization.testkit.{UnitSpec, WireMockFixture}
import com.sksamuel.avro4s.{AvroOutputStream, FromRecord, SchemaFor, ToRecord}
import org.apache.avro.Schema

object Avro4sSerializationSpec {

  import com.ovoenergy.kafka.serialization.testkit.UnitSpec._

  implicit val eventToRecord: ToRecord[Event] = ToRecord[Event]
}

class Avro4sSerializationSpec extends UnitSpec with WireMockFixture {

  import Avro4sSerializationSpec._
  import com.ovoenergy.kafka.serialization.testkit.UnitSpec._
  import WireMock._

  implicit val eventFromRecord: FromRecord[Event] = FromRecord[Event]
  implicit val eventSchemaFor: SchemaFor[Event] = SchemaFor[Event]

  "Avro4sSerialization" when {
    "serializing binary" when {
      "is value serializer" should {
        "register the schema to the schemaRegistry for value" in forAll {  (topic: String, event: Event) =>

          testWithPostSchemaExpected( s"$topic-value") {
            val serializer = avroBinarySchemaIdSerializer(wireMockEndpoint, isKey = false)
            serializer.serialize(topic, event)
          }
        }

        "register the schema to the schemaRegistry for value only once" in forAll { (topic: String, events: List[Event]) =>

          whenever(events.length > 1) {
            // This verifies that the HTTP cal to the schema registry happen only once.
            testWithPostSchemaExpected(s"$topic-value") {
              val serializer = avroBinarySchemaIdSerializer(wireMockEndpoint, isKey = false)
              events.foreach(event => serializer.serialize(topic, event))
            }
          }
        }

      }

      "is key serializer" should {
        "register the schema to the schemaRegistry for key" in forAll { (topic: String, event: Event) =>
          testWithPostSchemaExpected(s"$topic-key") {
            val serializer = avroBinarySchemaIdSerializer(wireMockEndpoint, isKey = true)
            serializer.serialize(topic, event)
          }
        }
      }
    }

    "serializing json" when {
      "the value is serializer" should {
        "register the schema to the schemaRegistry for value" in forAll { (topic: String, event: Event) =>

          testWithPostSchemaExpected(s"$topic-value") {
            val serializer = avroJsonSchemaIdSerializer(wireMockEndpoint, isKey = false)
            serializer.serialize(topic, event)
          }
        }

        "register the schema to the schemaRegistry for value only once" in forAll { (topic: String, events: List[Event]) =>

          whenever(events.length > 1) {
            // This verifies that the HTTP cal to the schema registry happen only once.
            testWithPostSchemaExpected(s"$topic-value") {
              val serializer = avroJsonSchemaIdSerializer(wireMockEndpoint, isKey = false)
              events.foreach(event => serializer.serialize(topic, event))
            }
          }
        }
      }

      "is key serializer" should {
        "register the schema to the schemaRegistry for key" in forAll { (topic: String, event: Event) =>

          testWithPostSchemaExpected(s"$topic-key") {
            val serializer = avroJsonSchemaIdSerializer(wireMockEndpoint, isKey = true)
            serializer.serialize(topic, event)
          }
        }
      }
    }

    "deserializing binary" when {
      "the writer schema is used" should {
        "read the schema from the registry" in forAll { (topic: String, schemaId: Int, event: Event) =>

          val deserializer = avroBinarySchemaIdDeserializer(wireMockEndpoint, isKey = false)

          val bytes = asAvroBinaryWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, eventSchemaFor())

          val deserialized = deserializer.deserialize(topic, bytes)
          deserialized shouldBe event
        }

        "read the schema from the registry only once" in forAll { (topic: String, schemaId: Int, event: Event) =>

          // The looping nature of scalacheck causes to reuse the same wiremock configuration
          resetWireMock()

          val deserializer = avroBinarySchemaIdDeserializer(wireMockEndpoint, isKey = false)
          val bytes = asAvroBinaryWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, eventSchemaFor())

          deserializer.deserialize(topic, bytes)
          deserializer.deserialize(topic, bytes)

          verify(1, getRequestedFor(urlMatching(s"/schemas/ids/$schemaId")))
        }
      }
    }

    "deserializing json" when {
      "the writer schema is used" should {
        "read the schema from the registry" in forAll { (topic: String, schemaId: Int, event: Event) =>

          val deserializer = avroJsonSchemaIdDeserializer(wireMockEndpoint, isKey = false)
          val bytes = asAvroJsonWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, eventSchemaFor())

          val deserialized = deserializer.deserialize(topic, bytes)

          deserialized shouldBe event
        }

        "read the schema from the registry only once" in forAll { (topic: String, schemaId: Int, event: Event) =>

          // The looping nature of scalacheck causes to reuse the same wiremock configuration
          resetWireMock()

          val deserializer = avroJsonSchemaIdDeserializer(wireMockEndpoint, isKey = false)
          val bytes = asAvroJsonWithSchemaIdBytes(event, schemaId)

          givenSchema(schemaId, eventSchemaFor())

          deserializer.deserialize(topic, bytes)
          deserializer.deserialize(topic, bytes)

          verify(1, getRequestedFor(urlMatching(s"/schemas/ids/$schemaId")))
        }
      }
    }
  }

  private def givenSchema[T: SchemaFor](schemaId: Int, schema: Schema) = {

    // The schema property is a string containing JSON.
    val schemaBody = "{\"schema\": \"" + schema.toString.replace(""""""", """\"""") + "\"}"


    stubFor(
      get(urlMatching(s"/schemas/ids/$schemaId"))
        .willReturn(aResponse()
          .withBody(schemaBody)
          .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )
  }

  private def asAvroBinaryWithSchemaIdBytes[T: SchemaFor : ToRecord](t: T, schemaId: Int): Array[Byte] = {
    asAvroWithSchemaIdBytes(t, schemaId, AvroOutputStream.binary[T])
  }

  private def asAvroJsonWithSchemaIdBytes[T: SchemaFor : ToRecord](t: T, schemaId: Int): Array[Byte] = {
    asAvroWithSchemaIdBytes(t, schemaId, AvroOutputStream.json[T])
  }

  private def asAvroWithSchemaIdBytes[T: SchemaFor : ToRecord](t: T, schemaId: Int, mkAvroOut: OutputStream => AvroOutputStream[T]): Array[Byte] = {
    val bout = new ByteArrayOutputStream()
    val avroOut = mkAvroOut(bout)
    avroOut.write(t)
    avroOut.flush()
    ByteBuffer.allocate(bout.size()+ 4).putInt(schemaId).put(bout.toByteArray).array()
  }


  private def testWithPostSchemaExpected[T](subject: String)(f: => T) = {

    stubFor(
      post(urlMatching("/subjects/.*/versions"))
        .willReturn(aResponse()
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
