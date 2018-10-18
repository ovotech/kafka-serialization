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
import com.ovoenergy.kafka.serialization.avro.{JerseySchemaRegistryClient, SchemaRegistryClientSettings}
import com.ovoenergy.kafka.serialization.core._
import com.sksamuel.avro4s._
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
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
  def avroBinarySchemaIdDeserializer[T <: Product: FromRecord](schemaRegistryEndpoint: String,
                                                               isKey: Boolean,
                                                               includesFormatByte: Boolean): KafkaDeserializer[T] =
    avroBinarySchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdDeserializer[T <: Product: FromRecord](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdDeserializerWithProps(
      schemaRegistryClient,
      isKey,
      () => schemaRegistryClient.close(),
      includesFormatByte
    )
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroDeserializer]]
    */
  def avroBinarySchemaIdDeserializer[T <: Product: FromRecord](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  ): KafkaDeserializer[T] =
    avroBinarySchemaIdDeserializerWithProps(schemaRegistryClient, isKey, () => Unit, includesFormatByte, props)

  private def avroBinarySchemaIdDeserializerWithProps[T <: Product](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  )(implicit fromRecord: FromRecord[T]): KafkaDeserializer[T] = {

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

        fromRecord.from(d.deserialize(topic, bytes).asInstanceOf[GenericRecord])
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
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T <: Product: FromRecord: SchemaFor](
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
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T <: Product: FromRecord: SchemaFor](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdWithReaderSchemaDeserializerWithProps(
      schemaRegistryClient,
      isKey,
      () => schemaRegistryClient.close(),
      includesFormatByte
    )
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID,
    * allowing you to specify a reader schema.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroDeserializer]]
    */
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T <: Product: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  ): KafkaDeserializer[T] =
    avroBinarySchemaIdWithReaderSchemaDeserializerWithProps(
      schemaRegistryClient,
      isKey,
      () => Unit,
      includesFormatByte,
      props
    )

  private def avroBinarySchemaIdWithReaderSchemaDeserializerWithProps[T <: Product: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  )(implicit fromRecord: FromRecord[T], schemaFor: SchemaFor[T]): KafkaDeserializer[T] = {

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

        fromRecord.from(d.deserialize(topic, bytes, schemaFor.schema).asInstanceOf[GenericRecord])
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
  def avroBinarySchemaIdSerializer[T <: Product: ToRecord](schemaRegistryEndpoint: String,
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
  def avroBinarySchemaIdSerializer[T <: Product: ToRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                           isKey: Boolean,
                                                           includesFormatByte: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdSerializerWithProps(
      schemaRegistryClient,
      isKey,
      () => schemaRegistryClient.close(),
      includesFormatByte
    )
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
  def avroBinarySchemaIdSerializer[T <: Product: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                           isKey: Boolean,
                                                           includesFormatByte: Boolean,
                                                           props: Map[String, String] = Map()): KafkaSerializer[T] =
    avroBinarySchemaIdSerializerWithProps(schemaRegistryClient, isKey, () => Unit, includesFormatByte, props)

  private def avroBinarySchemaIdSerializerWithProps[T <: Product](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  )(implicit toRecord: ToRecord[T]): KafkaSerializer[T] = {

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
      val bytes = kafkaAvroSerializer.serialize(topic, toRecord.to(t))
      if (includesFormatByte)
        bytes
      else
        dropMagicByte(bytes)
    }, close)
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T <: Product: FromRecord: SchemaFor](
    schemaRegistryEndpoint: String,
    isKey: Boolean
  ): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdDeserializerWithReaderSchema[T <: Product: FromRecord: SchemaFor](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T <: Product: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean
  ): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, () => Unit)

  private def avroJsonSchemaIdDeserializerWithReaderSchema[T <: Product: FromRecord](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit
  )(implicit fromRecord: FromRecord[T], schemaFor: SchemaFor[T]): KafkaDeserializer[T] =
    formatCheckingDeserializer(Format.AvroJsonSchemaId, deserializer({ (_, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val writerSchema = schemaRegistryClient.getById(schemaId)

      val reader = new GenericDatumReader[GenericRecord](writerSchema, schemaFor.schema)
      val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, new ByteBufferBackedInputStream(buffer))

      val readRecord = reader.read(null, jsonDecoder)

      fromRecord.from(readRecord)
    }, close))

  def avroJsonSchemaIdDeserializer[T <: Product: FromRecord](schemaRegistryEndpoint: String,
                                                             isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdDeserializer[T <: Product: FromRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                             isKey: Boolean): KafkaDeserializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
  }

  def avroJsonSchemaIdDeserializer[T <: Product: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                             isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey, () => Unit)

  private def avroJsonSchemaIdDeserializer[T <: Product](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit
  )(implicit fromRecord: FromRecord[T]): KafkaDeserializer[T] =
    deserializer({ (topic, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val writerSchema = schemaRegistryClient.getById(schemaId)

      val reader = new GenericDatumReader[GenericRecord](writerSchema)
      val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, new ByteBufferBackedInputStream(buffer))

      val readRecord = reader.read(null, jsonDecoder)

      fromRecord.from(readRecord)
    }, close)

  def avroJsonSchemaIdSerializer[T <: Product: ToRecord](schemaRegistryEndpoint: String,
                                                         isKey: Boolean): KafkaSerializer[T] =
    avroJsonSchemaIdSerializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdSerializer[T <: Product: ToRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                         isKey: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
  }

  def avroJsonSchemaIdSerializer[T <: Product: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                         isKey: Boolean): KafkaSerializer[T] =
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey, () => Unit)

  private def avroJsonSchemaIdSerializer[T <: Product: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                                 isKey: Boolean,
                                                                 close: () => Unit): KafkaSerializer[T] = {

    val toRecord: ToRecord[T] = implicitly

    serializer({ (topic, t) =>
      val record = toRecord.to(t)

      val schemaId = schemaRegistryClient.register(s"$topic-${if (isKey) "key" else "value"}", record.getSchema)

      // TODO size hint
      val bout = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[GenericRecord](record.getSchema)
      val jsonEncoder = EncoderFactory.get.jsonEncoder(record.getSchema, bout)

      writer.write(toRecord.to(t), jsonEncoder)

      bout.flush()
      bout.close()

      ByteBuffer.allocate(4).putInt(schemaId).array() ++ bout.toByteArray
    }, close)
  }
}
