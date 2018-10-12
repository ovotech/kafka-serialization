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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.ovoenergy.kafka.serialization.testkit.WireMockFixture
import org.apache.avro.Schema
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SchemaRegistryFixture extends BeforeAndAfterEach { _: Suite with WireMockFixture =>

  def schemaRegistryEndpoint: String = wireMockEndpoint

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    stubFor(
      get(urlMatching(s"/schemas/ids/.*"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{
                        |  "error_code": 40403,
                        |  "message": "Schema not found"
                        |}
                      """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
        .atPriority(Int.MaxValue)
    )

    stubFor(
      get(urlMatching(s"/subjects/.*/versions"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{
                        |   "error_code": 40401,
                        |   "message": "Subject not found."
                        |}
                      """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
        .atPriority(Int.MaxValue)

    )

    stubFor(
      get(urlMatching(s"/subjects/.*/versions/.*"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{
                        |   "error_code": 40403,
                        |   "message": "Version not found."
                        |}
                      """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
        .atPriority(Int.MaxValue)
    )

  }

  def givenSchema(schemaId: Int, schema: Schema): Unit = {

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

  def givenBasicAuthenticationIsNeeded = {
    stubFor(
      any(anyUrl)
        .withHeader("Authorization", absent())
        .willReturn(
          aResponse()
            .withStatus(401)
        )
    )
  }

  def givenNonExistingSchema(schemaId: Int): Unit =
    stubFor(
      get(urlMatching(s"/schemas/ids/$schemaId"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{
              |  "error_code": 40403,
              |  "message": "Schema not found"
              |}
            """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

  def givenNonExistingSubject(subject: String): Unit = {
    stubFor(
      get(urlMatching(s"/subjects/$subject/versions"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{
                        |   "error_code": 40401,
                        |   "message": "Subject not found."
                        |}
                      """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

    stubFor(
      get(urlMatching(s"/subjects/$subject/versions/.*"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{
                        |   "error_code": 40401,
                        |   "message": "Subject not found."
                        |}
                      """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )
  }

  def givenSubjectLastVersionId(subject: String, lastVersionId: Int): Unit = {

    val versions = (1 to lastVersionId).toList

    stubFor(
      get(urlMatching(s"/subjects/$subject/versions"))
        .willReturn(
          aResponse()
            .withBody(versions.mkString("[", ",", "]"))
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )
  }

  def givenSubjectVersion(subject: String, versionId: Int, schemaId: Int, schema: Schema, isLast: Boolean = false): Unit = {

    val escapedSchema = schema.toString.replace(""""""", """\"""")

    stubFor(
      get(urlMatching(s"/subjects/$subject/versions/$versionId"))
        .willReturn(
          aResponse()
            .withBody(
              s"""
                |{
                |  "subject": "$subject",
                |  "id": $schemaId,
                |  "version": $versionId,
                |  "schema": "$escapedSchema"
                |}
              """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

    stubFor(
      get(urlMatching(s"/subjects/$subject/versions/latest"))
        .willReturn(
          aResponse()
            .withBody(
              s"""
                 |{
                 |  "subject": "$subject",
                 |  "id": $schemaId,
                 |  "version": $versionId,
                 |  "schema": "$escapedSchema"
                 |}
              """.stripMargin)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )
  }


  // TODO match on the schema
  def givenNextSchemaId(subject: String, schemaId: Int): Unit =
    stubFor(
      post(urlMatching(s"/subjects/$subject/versions"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("{\"id\": " + schemaId + "}")
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

  // TODO verify the schema is the same
  def verifySchemaHasBeenPosted(subject: String) =
    verify(postRequestedFor(urlEqualTo(s"/subjects/$subject/versions")))

  def givenNextError(status: Int, errorCode: Int, errorMessage: String): Unit =
    stubFor(
      any(anyUrl())
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody("{\"error_code\": " + errorCode + ", \"message\": \"" + errorMessage + "\"}")
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

  def givenHtmlResponse(status: Int, body: String): Unit =
    stubFor(
      any(anyUrl())
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
            .withHeader("Content-Type", "text/html")
        )
    )

  def givenJsonResponse(status: Int, body: String, url: String): Unit =
    stubFor(
      any(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
    )

}
