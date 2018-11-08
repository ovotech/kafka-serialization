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

package com.ovoenergy.kafka.serialization.avro4s

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.ovoenergy.kafka.serialization.avro.{Authentication, SchemaRegistryClientSettings}
import com.ovoenergy.kafka.serialization.core._
import com.sksamuel.avro4s._
import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClient}
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

import scala.collection.JavaConverters._

private[avro4s] trait Avro4sSerialization {

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * Assumes that the schema registry does not require authentication.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryEndpoint: String,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean): KafkaDeserializer[T] =
    avroBinarySchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdDeserializerWithProps(schemaRegistryClient, isKey, () => (), includesFormatByte)
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroDeserializer]]
    */
  def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean,
                                                    props: Map[String, String] = Map()): KafkaDeserializer[T] =
    avroBinarySchemaIdDeserializerWithProps(schemaRegistryClient, isKey, () => Unit, includesFormatByte, props)

  private def avroBinarySchemaIdDeserializerWithProps[T: FromRecord](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  ): KafkaDeserializer[T] = {

    val fromRecord = implicitly[FromRecord[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient)

    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    val properties = props.get("schema.registry.url") match {
      case Some(_) => props.asJava
      case None    => (props + ("schema.registry.url" -> "")).asJava
    }
    d.configure(properties, isKey)

    deserializer(
      { (topic, data) =>
        val bytes = {
          if (includesFormatByte) data
          else Array(0: Byte) ++ data // prepend the magic byte before delegating to the KafkaAvroDeserializer
        }

        fromRecord(d.deserialize(topic, bytes).asInstanceOf[GenericRecord])
      },
      close
    )
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID,
    * allowing you to specify a reader schema.
    *
    * Assumes that the schema registry does not require authentication.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord: SchemaFor](
    schemaRegistryEndpoint: String,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] =
    avroBinarySchemaIdWithReaderSchemaDeserializer(
      SchemaRegistryClientSettings(schemaRegistryEndpoint),
      isKey,
      includesFormatByte
    )

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID,
    * allowing you to specify a reader schema.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord: SchemaFor](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdWithReaderSchemaDeserializerWithProps(schemaRegistryClient, isKey, () => (), includesFormatByte)
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID,
    * allowing you to specify a reader schema.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroDeserializer]]
    */
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  ): KafkaDeserializer[T] =
    avroBinarySchemaIdWithReaderSchemaDeserializerWithProps(
      schemaRegistryClient,
      isKey,
      () => (),
      includesFormatByte,
      props
    )

  @deprecated("There is no need to close the client")
  private def avroBinarySchemaIdWithReaderSchemaDeserializerWithProps[T: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  ): KafkaDeserializer[T] = {

    val fromRecord = implicitly[FromRecord[T]]
    val schemaFor = implicitly[SchemaFor[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient)

    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    val properties = props.get("schema.registry.url") match {
      case Some(_) => props.asJava
      case None    => (props + ("schema.registry.url" -> "")).asJava
    }

    d.configure(properties, isKey)

    deserializer(
      { (topic, data) =>
        val bytes = {
          if (includesFormatByte) data
          else Array(0: Byte) ++ data // prepend the magic byte before delegating to the KafkaAvroDeserializer
        }

        fromRecord(d.deserialize(topic, bytes, schemaFor()).asInstanceOf[GenericRecord])
      },
      close
    )
  }

  /**
    * Build a serializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * Assumes that the schema registry does not require authentication.
    *
    * @param isKey true if this is a serializer for keys, false if it is for values
    * @param includesFormatByte whether the messages should be prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryEndpoint: String,
                                                isKey: Boolean,
                                                includesFormatByte: Boolean): KafkaSerializer[T] =
    avroBinarySchemaIdSerializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  /**
    * Build a serializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * Assumes that the schema registry does not require authentication.
    *
    * @param isKey true if this is a serializer for keys, false if it is for values
    * @param includesFormatByte whether the messages should be prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                isKey: Boolean,
                                                includesFormatByte: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdSerializerWithProps(schemaRegistryClient, isKey, () => (), includesFormatByte)
  }

  /**
    * Build a serializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * Assumes that the schema registry does not require authentication.
    *
    * @param isKey true if this is a serializer for keys, false if it is for values
    * @param includesFormatByte whether the messages should be prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroSerializer]]
    */
  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                isKey: Boolean,
                                                includesFormatByte: Boolean,
                                                props: Map[String, String] = Map()): KafkaSerializer[T] =
    avroBinarySchemaIdSerializerWithProps(schemaRegistryClient, isKey, () => Unit, includesFormatByte, props)

  private def avroBinarySchemaIdSerializerWithProps[T: ToRecord](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  ): KafkaSerializer[T] = {
    val toRecord = implicitly[ToRecord[T]]

    val kafkaAvroSerializer = new KafkaAvroSerializer(schemaRegistryClient)

    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    val properties = props.get("schema.registry.url") match {
      case Some(_) => props.asJava
      case None    => (props + ("schema.registry.url" -> "")).asJava
    }
    kafkaAvroSerializer.configure(properties, isKey)

    def dropMagicByte(bytes: Array[Byte]): Array[Byte] =
      if (bytes != null && bytes.nonEmpty) {
        bytes.drop(1)
      } else {
        bytes
      }

    serializer({ (topic, t) =>
      val bytes = kafkaAvroSerializer.serialize(topic, toRecord(t))
      if (includesFormatByte)
        bytes
      else
        dropMagicByte(bytes)
    }, close)
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryEndpoint: String,
                                                                  isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, () => ())
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                                  isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, () => Unit)

  @deprecated("There is no need to close the client")
  private def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                                          isKey: Boolean,
                                                                          close: () => Unit): KafkaDeserializer[T] =
    formatCheckingDeserializer(Format.AvroJsonSchemaId, deserializer({ (topic, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val schema = schemaRegistryClient.getById(schemaId)

      implicit val schemaFor: SchemaFor[T] = new SchemaFor[T] {
        override def apply(): Schema = schema
      }

      val avroIn = AvroJsonInputStream[T](new ByteBufferBackedInputStream(buffer))

      avroIn.singleEntity.get
    }, close))

  def avroJsonSchemaIdDeserializer[T: FromRecord: SchemaFor](schemaRegistryEndpoint: String,
                                                             isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdDeserializer[T: FromRecord: SchemaFor](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                             isKey: Boolean): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey, () => ())
  }

  def avroJsonSchemaIdDeserializer[T: FromRecord: SchemaFor](schemaRegistryClient: SchemaRegistryClient,
                                                             isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey, () => Unit)

  private def avroJsonSchemaIdDeserializer[T: FromRecord: SchemaFor](schemaRegistryClient: SchemaRegistryClient,
                                                                     isKey: Boolean,
                                                                     close: () => Unit): KafkaDeserializer[T] =
    deserializer({ (topic, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val writerSchema = schemaRegistryClient.getById(schemaId)
      val readerSchema = implicitly[SchemaFor[T]].apply()

      val reader = new GenericDatumReader[GenericRecord](writerSchema, readerSchema)
      val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, new ByteBufferBackedInputStream(buffer))

      val readRecord = reader.read(null, jsonDecoder)

      implicitly[FromRecord[T]].apply(readRecord)
    }, close)

  def avroJsonSchemaIdSerializer[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] =
    avroJsonSchemaIdSerializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdSerializer[T: ToRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                              isKey: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey, () => ())
  }

  def avroJsonSchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                              isKey: Boolean): KafkaSerializer[T] =
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey, () => Unit)

  private def avroJsonSchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                      isKey: Boolean,
                                                      close: () => Unit): KafkaSerializer[T] = {

    val toRecord: ToRecord[T] = implicitly

    serializer({ (topic, t) =>
      val record = toRecord(t)
      implicit val schemaFor: SchemaFor[T] = new SchemaFor[T] {
        override def apply(): Schema = record.getSchema
      }

      val schemaId = schemaRegistryClient.register(s"$topic-${if (isKey) "key" else "value"}", record.getSchema)

      // TODO size hint
      val bout = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[GenericRecord](record.getSchema)
      val jsonEncoder = EncoderFactory.get.jsonEncoder(record.getSchema, bout)

      writer.write(toRecord(t), jsonEncoder)

      bout.flush()
      bout.close()

      ByteBuffer.allocate(4).putInt(schemaId).array() ++ bout.toByteArray
    }, close)
  }

  private def initSchemaRegistryClient(settings: SchemaRegistryClientSettings): SchemaRegistryClient = {
    val config = settings.authentication match {
      case Authentication.Basic(username, password) =>
        Map("basic.auth.credentials.source" -> "USER_INFO", "basic.auth.user.info" -> s"$username:$password")
      case Authentication.None =>
        Map.empty[String, String]
    }

    new CachedSchemaRegistryClient(settings.endpoint, settings.maxCacheSize, config.asJava)
  }
}
