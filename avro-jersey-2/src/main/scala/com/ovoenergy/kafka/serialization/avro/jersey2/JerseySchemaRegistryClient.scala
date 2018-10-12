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

package com.ovoenergy.kafka.serialization.avro.jersey2

import java.util
import java.util.concurrent.ExecutionException

import com.ovoenergy.kafka.serialization.avro.jersey2.JerseySchemaRegistryClient.SchemaCacheKey
import com.ovoenergy.kafka.serialization.avro.{Authentication, SchemaRegistryClientSettings}
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.schemaregistry.client.{SchemaMetadata, SchemaRegistryClient}
import javax.json.{Json, JsonArray, JsonObject, JsonString}
import javax.ws.rs.client.{Client, ClientBuilder, Entity}
import javax.ws.rs.core.Response
import jersey.repackaged.com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import org.apache.avro.Schema
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Implements the [[SchemaRegistryClient]] interface using Jersey client.
  *
  * It caches the schema and schema id locally to avoid issueing a new call for each request. This behavior is the same
  * of the confluent [[io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient]].
  *
  * It implements only a subset of the [[SchemaRegistryClient]] interface.
  */
class JerseySchemaRegistryClient(settings: SchemaRegistryClientSettings)
    extends SchemaRegistryClient
    with AutoCloseable {

  private val logger = LoggerFactory.getLogger(getClass)

  // This cache the schema id by subject and schema
  private val subjectSchemaCache: LoadingCache[SchemaCacheKey, java.lang.Integer] = CacheBuilder
    .newBuilder()
    .maximumSize(settings.maxCacheSize)
    .concurrencyLevel(settings.cacheParallelism)
    .build(new CacheLoader[SchemaCacheKey, java.lang.Integer] {
      override def load(key: SchemaCacheKey): java.lang.Integer = {
        val entity = Json
          .createObjectBuilder()
          .add("schema", key.schema.toString)
          .build()

        processResponse(
          subjectVersions(key.subject)
            .request()
            .post(Entity.entity(entity, "application/vnd.schemaregistry.v1+json"))
        ) { response =>
          response.readEntity(classOf[JsonObject]).getInt("id")
        }
      }
    })

  // This cache the schema by schema id
  private val schemaCache: LoadingCache[java.lang.Integer, Schema] = CacheBuilder
    .newBuilder()
    .maximumSize(settings.maxCacheSize)
    .concurrencyLevel(settings.cacheParallelism)
    .build(new CacheLoader[java.lang.Integer, Schema] {
      override def load(id: java.lang.Integer): Schema =
        processResponse(
          schemas
            .path(id.toString)
            .request()
            .get()
        ) { response =>
          new Schema.Parser().parse(response.readEntity(classOf[JsonObject]).getString("schema"))
        }
    })

  private val client: Client = {
    var builder = ClientBuilder
      .newBuilder()

    settings.authentication match {
      case Authentication.Basic(username, password) =>
        val authFeature = HttpAuthenticationFeature.basicBuilder().credentials(username, password).build()

        builder = JerseyClientRegisterHack.register(builder, authFeature)

        builder.register()
      case Authentication.None =>
    }

    builder.build()
  }

  private val root = client.target(settings.endpoint)
  private val subjects = root.path("subjects")
  private val schemas = root.path("schemas").path("ids")

  private def subjectVersions(subject: String) =
    subjects
      .path(subject)
      .path("versions")

  override def getAllSubjects: util.Collection[String] =
    processResponse(
      subjects
        .request()
        .get()
    ) { response =>
      response
        .readEntity(classOf[JsonArray])
        .getValuesAs(classOf[JsonString])
        .asScala
        .map(_.getString)
        .asJavaCollection
    }

  override def getBySubjectAndId(subject: String, id: Int): Schema =
    getBySubjectAndID(subject, id)

  override def getBySubjectAndID(subject: String, id: Int): Schema =
    getByID(id)

  override def register(subject: String, schema: Schema): Int =
    Try {
      subjectSchemaCache.get(SchemaCacheKey(subject, schema))
    }.recoverWith {
      case e: ExecutionException => Failure(e.getCause)
    } match {
      case Failure(e)   => throw e
      case Success(int) => int
    }

  override def getById(id: Int): Schema =
    getByID(id)

  override def getByID(id: Int): Schema =
    Try {
      schemaCache.get(id)
    }.recoverWith {
      case e: ExecutionException => Failure(e.getCause)
    } match {
      case Failure(e)      => throw e
      case Success(schema) => schema
    }

  override def getCompatibility(subject: String): String =
    throw new UnsupportedOperationException

  override def updateCompatibility(subject: String, s1: String): String =
    throw new UnsupportedOperationException

  override def getVersion(subject: String, schema: Schema): Int =
    throw new UnsupportedOperationException

  override def getSchemaMetadata(subject: String, id: Int): SchemaMetadata =
    throw new UnsupportedOperationException

  override def getLatestSchemaMetadata(subject: String): SchemaMetadata =
    processResponse(
      subjects
        .path(subject)
        .path("versions")
        .path("latest")
        .request()
        .get()
    ) { response =>
      val jsonObject = response.readEntity(classOf[JsonObject])
      val id = jsonObject.getInt("id")
      val version = jsonObject.getInt("version")
      val schema = jsonObject.getString("schema")
      new SchemaMetadata(id, version, schema)
    }

  override def testCompatibility(subject: String, schema: Schema): Boolean =
    throw new UnsupportedOperationException

  override def close(): Unit =
    client.close()

  private def processResponse[T](r: Response)(f: Response => T) =
    try {
      if (r.getStatus == 200) {
        f(r)
      } else {
        throw parseRestException(r)
      }
    } finally {
      r.close()
    }

  private def parseRestException(response: Response) = {
    response.bufferEntity() // so we can read it more than once
    try {
      val jsonObject = response.readEntity(classOf[JsonObject])
      new RestClientException(jsonObject.getString("message"), response.getStatus, jsonObject.getInt("error_code"))
    } catch {
      case NonFatal(_) =>
        val truncatedResponseBody = response.readEntity(classOf[String]).take(10000).mkString
        logger.warn(
          s"Schema registry returned a non-JSON.response. Status: ${response.getStatus}. Response (truncated): $truncatedResponseBody"
        )
        new RestClientException(
          "Server returned a non-JSON response. See the logs for details.",
          response.getStatus,
          -1
        )
    }
  }

  override def getId(subject: String, schema: Schema): Int =
    throw new UnsupportedOperationException

  override def deleteSchemaVersion(requestProperties: util.Map[String, String],
                                   subject: String,
                                   version: String): Integer =
    throw new UnsupportedOperationException

  override def deleteSchemaVersion(subject: String, version: String): Integer =
    throw new UnsupportedOperationException

  override def deleteSubject(requestProperties: util.Map[String, String], subject: String): util.List[Integer] =
    throw new UnsupportedOperationException

  override def deleteSubject(subject: String): util.List[Integer] =
    throw new UnsupportedOperationException

  override def getAllVersions(subject: String): util.List[Integer] =
    throw new UnsupportedOperationException
}

object JerseySchemaRegistryClient {

  private case class SchemaCacheKey(subject: String, schema: Schema) {

    // The schema need to be compared by reference as it is mutable.
    override def equals(obj: scala.Any): Boolean = obj match {
      case that: SchemaCacheKey =>
        this.subject == that.subject && this.schema.eq(that.schema)
      case _ => false
    }

  }

  def apply(settings: SchemaRegistryClientSettings): JerseySchemaRegistryClient =
    new JerseySchemaRegistryClient(settings)
}
