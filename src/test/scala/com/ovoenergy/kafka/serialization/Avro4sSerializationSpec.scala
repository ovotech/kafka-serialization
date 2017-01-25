package com.ovoenergy.kafka.serialization

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.github.tomakehurst.wiremock.client.WireMock
import com.ovoenergy.{UnitSpec, WireMockFixture}
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import Avro4sSerialization._
import org.apache.avro.Schema
import org.scalatest.BeforeAndAfterEach
import com.sksamuel.avro4s.AvroOutputStream
import shapeless.{:+:, CNil, Coproduct, Generic}

object Avro4sSerializationSpec {

  case class Event(id: String, name: String)

  implicit val eventToRecord: ToRecord[Event] = ToRecord[Event]
}

class Avro4sSerializationSpec extends UnitSpec with WireMockFixture {

  import Avro4sSerializationSpec._
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
