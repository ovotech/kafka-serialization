package com.ovoenergy.kafka.serialization.avro

import com.github.tomakehurst.wiremock.client.WireMock._
import com.ovoenergy.kafka.serialization.testkit.WireMockFixture
import org.apache.avro.Schema
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SchemaRegistryFixture extends BeforeAndAfterEach { _: Suite with WireMockFixture =>

  def schemaRegistryEndpoint: String = wireMockEndpoint

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    // Some default behaviours

    stubFor(
      get(urlMatching(s"/schemas/ids/.*"))
        .atPriority(Int.MaxValue)
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

    stubFor(
      post(urlMatching("/subjects/.*/versions"))
        .atPriority(Int.MaxValue)
        .willReturn(
          aResponse()
            .withBody("{\"id\": 999}")
            .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        )
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

}
