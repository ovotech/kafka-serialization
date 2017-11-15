package com.ovoenergy.kafka.serialization.avro

import javax.ws.rs.core.HttpHeaders

import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.WireMock._
import com.ovoenergy.kafka.serialization.testkit.{UnitSpec, WireMockFixture}
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.apache.avro.{Schema, SchemaBuilder}

class JerseySchemaRegistryClientSpec extends UnitSpec with WireMockFixture with SchemaRegistryFixture {

  val schema: Schema = SchemaBuilder.record("Foo").fields().nullableBoolean("foo", false).endRecord()

  "JerseySchemaRegistryClient" when {
    "fetching a schema" when {
      "the authentication is basic" should {
        "send authorization header with the given credentials" in withJerseySchemaRegistryClient(
          SchemaRegistryClientSettings(wireMockEndpoint, "foo", "bar")
        ) { client =>
          val schemaId = 1

          givenSchema(schemaId, schema)
          client.getByID(schemaId)

          verify(getRequestedFor(anyUrl()).withBasicAuth(new BasicCredentials("foo", "bar")))
        }
      }

      "the authentication is None" should {
        "do not send authorization header" in withJerseySchemaRegistryClient { client =>
          val schemaId = 1

          givenSchema(schemaId, schema)
          client.getByID(schemaId)

          verify(getRequestedFor(anyUrl()).withoutHeader(HttpHeaders.AUTHORIZATION))
        }
      }

      "the schema exist" should {
        "fetch existing schema" in withJerseySchemaRegistryClient { client =>
          val schemaId = 1

          givenSchema(schemaId, schema)
          val fetchedSchema = client.getByID(schemaId)

          fetchedSchema shouldBe schema
        }

        "cache the fetched schema" in withJerseySchemaRegistryClient { client =>
          val schemaId = 1
          givenSchema(schemaId, schema)
          (0 until 10).map(_ => client.getByID(schemaId)) should contain allElementsOf List(schema)

          verify(1, getRequestedFor(anyUrl()))
        }
      }

      "the schema does not exist" should {
        "throw a RestClientException" in withJerseySchemaRegistryClient { client =>
          givenNonExistingSchema(1)

          a[RestClientException] should be thrownBy client.getByID(1)
        }
      }
    }
    "registering a schema" when {
      "the authentication is basic" should {
        "send authorization header with the given credentials" in withJerseySchemaRegistryClient(
          SchemaRegistryClientSettings(wireMockEndpoint, "foo", "bar")
        ) { client =>
          givenNextSchemaId("test-subject", 123)
          client.register("test-subject", schema)

          verify(postRequestedFor(anyUrl()).withBasicAuth(new BasicCredentials("foo", "bar")))
        }
      }

      "the authentication is None" should {
        "do not send authorization header" in withJerseySchemaRegistryClient { client =>
          givenNextSchemaId("test-subject", 123)
          client.register("test-subject", schema)

          verify(postRequestedFor(anyUrl()).withoutHeader(HttpHeaders.AUTHORIZATION))
        }
      }

      "return the schema id" in withJerseySchemaRegistryClient { client =>
        val schemaId = 321
        givenNextSchemaId("test-subject", schemaId)
        client.register("test-subject", schema) shouldBe schemaId
      }

      "throw an exception in case of error" in withJerseySchemaRegistryClient { client =>
        givenNextError(500, 123, "Error")
        a[RestClientException] should be thrownBy client.register("test-subject", schema)
      }

      "throw an exception in case of a non-JSON response" in withJerseySchemaRegistryClient { client =>
        givenHtmlResponse(401, "You're unauthorized, sucker!")
        a[RestClientException] should be thrownBy client.register("test-subject", schema)
      }

      "cache the registered schema" in withJerseySchemaRegistryClient { client =>
        val schemaId = 321
        givenNextSchemaId("test-subject", schemaId)
        (0 until 10).map(_ => client.register("test-subject", schema)) should contain allElementsOf List(schemaId)

        verify(1, postRequestedFor(anyUrl()))
      }
    }

    "getting the latest schema metadata" when {
      "it should return the latest schema" in withJerseySchemaRegistryClient { client =>
        val subject = "foo-value"
        val version = 2
        val id = 1
        val body = s"""{"subject":"$subject","version":$version,"id":$id,"schema":"\\"string\\""}"""
        givenJsonResponse(200, body, s"/subjects/$subject/versions/latest")

        val result = client.getLatestSchemaMetadata(subject)
        result.getSchema shouldBe ("\"string\"")
        result.getId shouldBe (id)
        result.getVersion shouldBe version
      }
    }
  }

  def withJerseySchemaRegistryClient[T](f: JerseySchemaRegistryClient => T): T =
    withJerseySchemaRegistryClient()(f)

  def withJerseySchemaRegistryClient[T](settings: SchemaRegistryClientSettings = SchemaRegistryClientSettings(
    wireMockEndpoint
  ))(f: JerseySchemaRegistryClient => T): T =
    resource.managed(JerseySchemaRegistryClient(settings)).acquireAndGet(f)

}
