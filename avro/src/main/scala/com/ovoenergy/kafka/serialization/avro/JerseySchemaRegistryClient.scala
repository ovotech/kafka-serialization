package com.ovoenergy.kafka.serialization.avro

import java.util
import java.util.concurrent.{Callable, ExecutionException}
import javax.json.{Json, JsonArray, JsonObject, JsonString}
import javax.ws.rs.client._
import javax.ws.rs.core.Response

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.schemaregistry.client.{SchemaMetadata, SchemaRegistryClient}
import jersey.repackaged.com.google.common.cache.{Cache, CacheBuilder, CacheLoader, LoadingCache}
import org.apache.avro.Schema
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import javax.ws.rs.client.ClientBuilder

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

class JerseySchemaRegistryClient(settings: SchemaRegistryClientSettings) extends SchemaRegistryClient with AutoCloseable {

  import JerseySchemaRegistryClient._

  private val subjectSchemaCache: LoadingCache[SchemaCacheKey, java.lang.Integer] = CacheBuilder
    .newBuilder()
    .maximumSize(settings.maxCacheSize)
    .concurrencyLevel(4)
    .build(new CacheLoader[SchemaCacheKey, java.lang.Integer] {
      override def load(key: SchemaCacheKey): java.lang.Integer = {
        val entity = Json.createObjectBuilder()
          .add("schema", key.schema.toString)
          .build()

        processResponse(subjectVersions(key.subject)
          .request()
          .post(Entity.entity(entity, "application/vnd.schemaregistry.v1+json"))) { response =>
          response.readEntity(classOf[JsonObject]).getInt("id")
        }
      }
    })

  private val schemaCache: LoadingCache[java.lang.Integer, Schema] = CacheBuilder
    .newBuilder()
    .maximumSize(settings.maxCacheSize)
    .concurrencyLevel(4)
    .build(new CacheLoader[java.lang.Integer, Schema] {
      override def load(id: java.lang.Integer): Schema = {
        processResponse(schemas
          .path(id.toString)
          .request()
          .get()) { response =>
          new Schema.Parser().parse(response.readEntity(classOf[JsonObject]).getString("schema"))
        }
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

  override def getAllSubjects: util.Collection[String] = {
    processResponse(subjects
      .request()
      .get()) { response =>
      response.readEntity(classOf[JsonArray]).getValuesAs(classOf[JsonString]).asScala.map(_.getString).asJavaCollection
    }
  }

  override def getBySubjectAndID(subject: String, id: Int): Schema =
    getByID(id)

  override def register(subject: String, schema: Schema): Int = Try {
    subjectSchemaCache.get(SchemaCacheKey(subject, schema))
  }.recoverWith {
    case e: ExecutionException => Failure(e.getCause)
  }.get

  override def getByID(id: Int): Schema = Try {
    schemaCache.get(id)
  }.recoverWith {
    case e: ExecutionException => Failure(e.getCause)
  }.get

  override def getCompatibility(subject: String): String =
    throw new UnsupportedOperationException

  override def updateCompatibility(subject: String, s1: String): String =
    throw new UnsupportedOperationException

  override def getVersion(subject: String, schema: Schema): Int =
    throw new UnsupportedOperationException

  override def getSchemaMetadata(subject: String, id: Int): SchemaMetadata =
    throw new UnsupportedOperationException

  override def getLatestSchemaMetadata(subject: String): SchemaMetadata =
    throw new UnsupportedOperationException

  override def testCompatibility(subject: String, schema: Schema): Boolean =
    throw new UnsupportedOperationException

  override def close(): Unit = {
    client.close()
  }

  private def processResponse[T](r: Response)(f: Response => T) = {
    if (r.getStatus == 200) {
      f(r)
    } else {
      throw parseRestException(r)
    }
  }

  private def parseRestException(response: Response) = {
    val jsonObject = response.readEntity(classOf[JsonObject])
    new RestClientException(jsonObject.getString("message"), response.getStatus, jsonObject.getInt("error_code"))
  }
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

  def apply(settings: SchemaRegistryClientSettings): JerseySchemaRegistryClient = new JerseySchemaRegistryClient(settings)
}