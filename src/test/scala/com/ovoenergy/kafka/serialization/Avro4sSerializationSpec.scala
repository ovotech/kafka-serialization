package com.ovoenergy.kafka.serialization

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.github.tomakehurst.wiremock.client.WireMock
import com.ovoenergy.kafka.serialization.Avro4sSerialization._
import com.ovoenergy.{UnitSpec, WireMockFixture}
import com.sksamuel.avro4s.{AvroOutputStream, FromRecord, SchemaFor, ToRecord}
import org.apache.avro.Schema

object Avro4sSerializationSpec {
  import UnitSpec._

  implicit val eventToRecord: ToRecord[Event] = ToRecord[Event]
}

class Avro4sSerializationSpec extends UnitSpec with WireMockFixture {
  import Avro4sSerializationSpec._
  import UnitSpec._
  import WireMock._


  "Avro4sSerialization" when {
    "serializing" when {
      "is value serializer" should {
        "register the schema to the schemaRegistry for value" in {

          val topic = "test-topic"
          val subject = s"$topic-value"

          testWithPostSchemaExpected(subject){
            val serializer = serializeWithAvroBinaryAndSchemaRegistry(wireMockEndpoint, isKey = false)
            serializer.serialize(topic, Event("test", "test"))
          }
        }

        "register the schema to the schemaRegistry for value only once" in {

          val topic = "test-topic"
          val subject = s"$topic-value"

          // This verifies that the HTTP cal to the schema regisrty happen only once.
          testWithPostSchemaExpected(subject){
            val serializer = serializeWithAvroBinaryAndSchemaRegistry(wireMockEndpoint, isKey = false)
            serializer.serialize(topic, Event("test_1", "test_1"))
            serializer.serialize(topic, Event("test_2", "test_2"))
            serializer.serialize(topic, Event("test_3", "test_3"))
          }
        }

      }

      "is key serializer" should {
        "register the schema to the schemaRegistry for key" in {

          val topic = "test-topic"
          val subject = s"$topic-key"

          testWithPostSchemaExpected(subject){
            val serializer = serializeWithAvroBinaryAndSchemaRegistry(wireMockEndpoint, isKey = true)
            serializer.serialize(topic, Event("test", "test"))
          }
        }
      }
    }

    "deserializing" when {
      "the writer schema is used" should {
        "read the schema from the registry" in {

          implicit val eventFromRecord: FromRecord[Event] = FromRecord[Event]
          implicit val eventSchemaFor: SchemaFor[Event] = SchemaFor[Event]

          val deserializer = deserializeAvroBinaryAndSchemaRegistry(wireMockEndpoint, isKey = false)

          val topic = "test-topic"
          val schemaId = 123

          val expectedEvent = Event("test", "test")
          val bytes = asAvroWithSchemaIdBytes(expectedEvent, schemaId)

          givenSchema(schemaId, eventSchemaFor())

          val deserialized = deserializer.deserialize(topic, bytes)
          deserialized shouldBe expectedEvent
        }

        "read the schema from the registry only once" in {

          implicit val eventFromRecord: FromRecord[Event] = FromRecord[Event]
          implicit val eventSchemaFor: SchemaFor[Event] = SchemaFor[Event]

          val deserializer = deserializeAvroBinaryAndSchemaRegistry(wireMockEndpoint, isKey = false)

          val topic = "test-topic"
          val schemaId = 123

          val expectedEvent = Event("test", "test")
          val bytes = asAvroWithSchemaIdBytes(expectedEvent, schemaId)

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

  private def asAvroWithSchemaIdBytes[T : SchemaFor : ToRecord](t: T, schemaId: Int): Array[Byte] = {
    val bout = new ByteArrayOutputStream()
    val avroOut = AvroOutputStream.binary[T](bout)
    avroOut.write(t)
    avroOut.flush()
    ByteBuffer.allocate(bout.size() + 1 + 4).put(0: Byte).putInt(schemaId).put(bout.toByteArray).array()
  }

  private def testWithPostSchemaExpected[T](subject: String)(f: =>T) = {

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
