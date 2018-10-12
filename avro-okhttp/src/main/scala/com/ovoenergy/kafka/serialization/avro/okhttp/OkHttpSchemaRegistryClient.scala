package com.ovoenergy.kafka.serialization.avro.okhttp

import java.io.IOException
import java.util

import com.eclipsesource.json.{Json, JsonObject}
import com.ovoenergy.kafka.serialization.avro.{Authentication, SchemaRegistryClientSettings}
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.schemaregistry.client.{SchemaMetadata, SchemaRegistryClient}
import okhttp3._
import org.apache.avro.Schema
import org.slf4j.LoggerFactory

import scala.util._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

class OkHttpSchemaRegistryClient(settings: SchemaRegistryClientSettings)
    extends SchemaRegistryClient
    with AutoCloseable {

  private val logger = LoggerFactory.getLogger(getClass)

  private val schemaRegistryMediaType = MediaType.parse("application/vnd.schemaregistry.v1+json")

  private val client = {

    val authenticator = settings.authentication match {
      case Authentication.Basic(username, password) =>
        new Authenticator {
          override def authenticate(route: Route, response: okhttp3.Response): Request = {

            if (response.request().header("Authorization") != null) {
              return null; // Give up, we've already failed to authenticate.
            }

            val credential = Credentials.basic(username, password)
            response
              .request()
              .newBuilder()
              .header("Authorization", credential)
              .build()
          }
        }
      case Authentication.None =>
        Authenticator.NONE
    }

    new OkHttpClient.Builder()
      .authenticator(authenticator)
      .addInterceptor(new Interceptor {
        override def intercept(chain: Interceptor.Chain): Response =
          chain.proceed(
            chain
              .request()
              .newBuilder()
              .addHeader("Accept", schemaRegistryMediaType.toString)
              .build()
          )
      })
      .build()

  }

  private val root = HttpUrl
    .parse(settings.endpoint)

  private val subjectsUri = root
    .newBuilder()
    .addPathSegment("subjects")

  private def subjectUri(s: String) =
    subjectsUri.addPathSegment(s)

  private def subjectVersionsUri(s: String) =
    subjectUri(s).addPathSegment("versions")

  private def subjectVersionUri(subject: String, versionId: Int) =
    subjectVersionsUri(subject).addPathSegment(versionId.toString)

  private def subjectLatestVersionsUri(s: String) =
    subjectVersionsUri(s).addPathSegment("latest")

  private val schemasUri = root
    .newBuilder()
    .addPathSegment("schemas")
    .addPathSegment("ids")

  private def schemaUri(id: Int) =
    schemasUri.addPathSegment(id.toString)

  @deprecated
  override def getBySubjectAndID(subject: String, id: Int): Schema =
    getBySubjectAndId(subject, id)

  @deprecated
  override def getByID(id: Int): Schema =
    getById(id)

  override def getAllSubjects: util.Collection[String] = {
    val request = new Request.Builder()
      .url(subjectsUri.build())
      .build()

    handleResponse(request) { response =>
      val array = Json.parse(response.body().charStream()).asArray()
      array.values().asScala.map(_.asString()).asJava
    }
  }

  override def getBySubjectAndId(subject: String, id: Int): Schema =
    getById(id)

  override def register(subject: String, schema: Schema): Int = {

    val jsonBody = Json.`object`().add("schema", schema.toString)

    val request = new Request.Builder()
      .url(subjectVersionsUri(subject).build())
      .post(RequestBody.create(schemaRegistryMediaType, jsonBody.toString))
      .build()

    handleResponse(request) { response =>
      val obj = Json.parse(response.body().charStream()).asObject()
      obj.get("id").asInt()
    }
  }

  override def getById(id: Int): Schema = {
    val request = new Request.Builder()
      .url(schemaUri(id).build())
      .build()

    handleResponse(request) { response =>
      val obj = Json.parse(response.body().charStream()).asObject()
      val schemaJson = obj.get("schema").asString()
      new Schema.Parser().parse(schemaJson)
    }
  }

  override def getCompatibility(subject: String): String =
    throw new UnsupportedOperationException

  override def updateCompatibility(subject: String, s1: String): String =
    throw new UnsupportedOperationException

  override def getVersion(subject: String, schema: Schema): Int =
    throw new UnsupportedOperationException

  override def getSchemaMetadata(subject: String, versionId: Int): SchemaMetadata = {
    val request = new Request.Builder()
      .url(subjectVersionUri(subject, versionId).build())
      .build()

    handleResponse(request) { r =>
      val obj = Json.parse(r.body().charStream()).asObject()
      parseSchemaMetadata(obj)
    }
  }

  override def getLatestSchemaMetadata(subject: String): SchemaMetadata = {
    val request = new Request.Builder()
      .url(subjectLatestVersionsUri(subject).build())
      .build()

    handleResponse(request) { r =>
      val obj = Json.parse(r.body().charStream()).asObject()
      parseSchemaMetadata(obj)
    }
  }

  private def parseSchemaMetadata(js: JsonObject): SchemaMetadata =
    new SchemaMetadata(js.get("id").asInt(), js.get("version").asInt(), js.get("schema").asString())

  override def testCompatibility(subject: String, schema: Schema): Boolean =
    throw new UnsupportedOperationException

  override def close(): Unit = {
    client.dispatcher().executorService().shutdown()
    client.connectionPool().evictAll()
    Option(client.cache()) foreach (_.close())
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

  override def getAllVersions(subject: String): util.List[Integer] = {
    val request = new Request.Builder()
      .url(subjectVersionsUri(subject).build())
      .build()

    handleResponse(request) { response =>
      val array = Json.parse(response.body().charStream()).asArray()
      array.values().asScala.map(_.asInt(): Integer).toList.asJava
    }
  }

  private def handleResponse[A](r: Request, client: OkHttpClient = client)(f: Response => A): A = {
    val tryA: Try[A] = Try(client.newCall(r).execute())
      .flatMap { response =>
        try {
          if (response.isSuccessful)
            Try(f(response))
          else
            parseError(response)
              .recover {
                case NonFatal(e) =>
                  val status =
                    Option(response.message()).map(msg => s"${response.code()} - $msg").getOrElse(s"${response.code()}")
                  val truncatedBody = Option(response.body()).map(_.string().take(1024))
                  logger.warn(
                    s"Schema registry returned a non-JSON.response. Status: $status, Response body (truncated): $truncatedBody"
                  )
                  new RestClientException(e.getMessage, response.code(), -1)
              }
              .flatMap(e => Failure[A](e))

        } finally {
          response.close()
        }
      }

    tryA.fold({
      case e: RestClientException => throw e
      case e: IOException         => throw e
      case e                      => throw new IOException(e)
    }, identity)
  }

  private def parseError(r: Response): Try[RestClientException] =
    Try {
      val obj = Json.parse(r.body().charStream()).asObject()
      new RestClientException(obj.get("message").asString(), r.code(), obj.get("error_code").asInt())
    }

}

object OkHttpSchemaRegistryClient {

  private case class SchemaCacheKey(subject: String, schema: Schema) {

    // The schema need to be compared by reference as it is mutable.
    override def equals(obj: scala.Any): Boolean = obj match {
      case that: SchemaCacheKey =>
        this.subject == that.subject && this.schema.eq(that.schema)
      case _ => false
    }

  }

  def apply(settings: SchemaRegistryClientSettings): OkHttpSchemaRegistryClient =
    new OkHttpSchemaRegistryClient(settings)
}
