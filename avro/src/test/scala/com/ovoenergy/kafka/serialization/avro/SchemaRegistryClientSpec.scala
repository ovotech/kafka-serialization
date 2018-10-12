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

package com.ovoenergy.kafka.serialization.avro

import javax.ws.rs.core.HttpHeaders
import com.github.tomakehurst.wiremock.client.{BasicCredentials, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.ovoenergy.kafka.serialization.testkit.{UnitSpec, WireMockFixture}
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.apache.avro.{Schema, SchemaBuilder}

import scala.util.Try

abstract class SchemaRegistryClientSpec extends UnitSpec with WireMockFixture with SchemaRegistryFixture {

  val schema: Schema = SchemaBuilder.record("Foo").fields().nullableBoolean("foo", false).endRecord()

  def implementationName: String

  implementationName when {
    "fetching a schema" when {
      "the authentication is basic" should {
        "send authorization header with the given credentials" in withSchemaRegistryClient(
          SchemaRegistryClientSettings(wireMockEndpoint, "foo", "bar")
        ) { client =>
          val schemaId = 1

          givenSchema(schemaId, schema)
          givenBasicAuthenticationIsNeeded

          Try(client.getById(schemaId))

          verify(getRequestedFor(anyUrl()).withBasicAuth(new BasicCredentials("foo", "bar")))
        }
      }

      "the authentication is None" should {
        "do not send authorization header" in withSchemaRegistryClient { client =>
          val schemaId = 1

          givenSchema(schemaId, schema)
          givenBasicAuthenticationIsNeeded

          an[Exception] should be thrownBy client.getById(schemaId)

          verify(0, anyRequestedFor(anyUrl()).withHeader("Authorization", matching(".*")))
        }
      }

      "the schema exist" should {
        "fetch existing schema" in withSchemaRegistryClient { client =>
          val schemaId = 1

          givenSchema(schemaId, schema)
          val fetchedSchema = client.getByID(schemaId)

          fetchedSchema shouldBe schema
        }

        "cache the fetched schema" in withSchemaRegistryClient { client =>
          val schemaId = 1
          givenSchema(schemaId, schema)
          (0 until 10).map(_ => client.getById(schemaId)) should contain allElementsOf List(schema)

          verify(1, getRequestedFor(anyUrl()))
        }
      }

      "the schema does not exist" should {
        "throw a RestClientException" in withSchemaRegistryClient { client =>
          givenNonExistingSchema(1)

          a[RestClientException] should be thrownBy client.getById(1)
        }
      }
    }
    "registering a schema" when {
      "the authentication is basic" should {
        "send authorization header with the given credentials" in withSchemaRegistryClient(
          SchemaRegistryClientSettings(wireMockEndpoint, "foo", "bar")
        ) { client =>
          givenNextSchemaId("test-subject", 123)
          givenBasicAuthenticationIsNeeded

          Try(client.register("test-subject", schema))

          verify(postRequestedFor(anyUrl()).withBasicAuth(new BasicCredentials("foo", "bar")))
        }
      }

      "the authentication is None" should {
        "not send authorization header" in withSchemaRegistryClient { client =>

          givenNextSchemaId("test-subject", 123)
          givenBasicAuthenticationIsNeeded

          an[Exception] should be thrownBy client.register("test-subject", schema)

          verify(0, anyRequestedFor(anyUrl()).withHeader("Authorization", matching(".*")))
        }
      }

      "return the schema id" in withSchemaRegistryClient { client =>
        val schemaId = 321
        givenNextSchemaId("test-subject", schemaId)
        client.register("test-subject", schema) shouldBe schemaId
      }

      "throw an exception in case of error" in withSchemaRegistryClient { client =>
        givenNextError(500, 123, "Error")
        a[RestClientException] should be thrownBy client.register("test-subject", schema)
      }

      "throw an exception in case of a non-JSON response" in withSchemaRegistryClient { client =>
        givenHtmlResponse(401, "You're unauthorized, sucker!")
        a[RestClientException] should be thrownBy client.register("test-subject", schema)
      }

      "cache the registered schema" in withSchemaRegistryClient { client =>
        val schemaId = 321
        givenNextSchemaId("test-subject", schemaId)
        (0 until 10).map(_ => client.register("test-subject", schema)) should contain allElementsOf List(schemaId)

        verify(1, postRequestedFor(anyUrl()))
      }
    }

    "getting the latest schema metadata" when {
      "the authentication is basic" should {
        "send authorization header with the given credentials" in withSchemaRegistryClient(
          SchemaRegistryClientSettings(wireMockEndpoint, "foo", "bar")
        ) { client =>

          val subject = "foo-value"
          val version = 2
          val id = 1
          val schema = SchemaBuilder.record("foo").fields().endRecord()
          givenSubjectVersion(subject, version, id, schema, isLast = true)
          givenBasicAuthenticationIsNeeded

          Try(client.getLatestSchemaMetadata(subject))

          verify(getRequestedFor(anyUrl()).withBasicAuth(new BasicCredentials("foo", "bar")))
        }
      }

      "the authentication is None" should {
        "do not send authorization header" in withSchemaRegistryClient { client =>

          val subject = "foo-value"
          val version = 2
          val id = 1
          val schema = SchemaBuilder.record("foo").fields().endRecord()
          givenSubjectVersion(subject, version, id, schema, isLast = true)
          givenBasicAuthenticationIsNeeded

          an[Exception] should be thrownBy client.getLatestSchemaMetadata(subject)

          verify(0, anyRequestedFor(anyUrl()).withHeader("Authorization", matching(".*")))
        }
      }

      "return the latest schema metadata" in withSchemaRegistryClient { client =>

        val subject = "foo-value"
        val version = 2
        val id = 1
        val schema = SchemaBuilder.record("foo").fields().endRecord()
        givenSubjectVersion(subject, version, id, schema, isLast = true)

        val result = client.getLatestSchemaMetadata(subject)

        new Schema.Parser().parse(result.getSchema) shouldBe schema
        result.getId shouldBe id
        result.getVersion shouldBe version
      }
    }
  }

  def withSchemaRegistryClient[T](f: SchemaRegistryClient => T): T =
    withSchemaRegistryClient()(f)

  def withSchemaRegistryClient[T](settings: SchemaRegistryClientSettings = SchemaRegistryClientSettings(
                                          wireMockEndpoint
                                        ))(f: SchemaRegistryClient => T): T

}
