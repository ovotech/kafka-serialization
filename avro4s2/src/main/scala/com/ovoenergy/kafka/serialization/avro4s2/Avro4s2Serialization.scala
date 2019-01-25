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

package com.ovoenergy.kafka.serialization
package avro4s2

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import scala.collection.JavaConverters._

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream

import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClient}
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}

import com.sksamuel.avro4s._

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

import avro.{Authentication, SchemaRegistryClientSettings}
import core._

private[avro4s2] trait Avro4s2Serialization {

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * Assumes that the schema registry does not require authentication.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdDeserializer[T: Decoder](schemaRegistryEndpoint: String,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean): KafkaDeserializer[T] =
    avroBinarySchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdDeserializer[T: Decoder](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdDeserializer(schemaRegistryClient, isKey, includesFormatByte)
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroDeserializer]]
    */
  def avroBinarySchemaIdDeserializer[T](schemaRegistryClient: SchemaRegistryClient,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean,
                                                    props: Map[String, String] = Map())(implicit decoder: Decoder[T]): KafkaDeserializer[T] = {

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

        val writerSchema = {
          val buffer = ByteBuffer.wrap(bytes)
          buffer.get() // Skip the magic byte
          val schemaId = buffer.getInt
          schemaRegistryClient.getById(schemaId)
        }

        decoder.decode(d.deserialize(topic, bytes), writerSchema)
      }
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
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: Decoder: SchemaFor](
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
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: Decoder: SchemaFor](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdWithReaderSchemaDeserializer(schemaRegistryClient, isKey, includesFormatByte)
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID,
    * allowing you to specify a reader schema.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    * @param props configuration of the underlying [[KafkaAvroDeserializer]]
    */
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    includesFormatByte: Boolean,
    props: Map[String, String] = Map()
  )(implicit decoder: Decoder[T], schemaFor: SchemaFor[T]): KafkaDeserializer[T] = {

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

        val buffer = ByteBuffer.wrap(bytes)
        val schemaId = {
          buffer.get() // Skip the magic byte
          buffer.getInt
        }

        val writerSchema = schemaRegistryClient.getById(schemaId)
        
        decoder.decode(d.deserialize(topic, bytes, schemaFor.schema), schemaFor.schema)
      }
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
  def avroBinarySchemaIdSerializer[T: Encoder: SchemaFor](schemaRegistryEndpoint: String,
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
  def avroBinarySchemaIdSerializer[T: Encoder: SchemaFor](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                isKey: Boolean,
                                                includesFormatByte: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdSerializer(schemaRegistryClient, isKey, includesFormatByte)
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
  def avroBinarySchemaIdSerializer[T](schemaRegistryClient: SchemaRegistryClient,
                                                isKey: Boolean,
                                                includesFormatByte: Boolean,
                                                props: Map[String, String] = Map.empty)(implicit encoder: Encoder[T], schemaFor: SchemaFor[T]): KafkaSerializer[T] = {

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
      val bytes = kafkaAvroSerializer.serialize(topic, encoder.encode(t, schemaFor.schema))
      if (includesFormatByte)
        bytes
      else
        dropMagicByte(bytes)
    })
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: Decoder: SchemaFor](schemaRegistryEndpoint: String,
                                                                  isKey: Boolean,
                                                                  includesFormatByte: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: Decoder: SchemaFor](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, includesFormatByte)
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T](schemaRegistryClient: SchemaRegistryClient,
                                                                  isKey: Boolean,
                                                                  includesFormatByte: Boolean)(implicit decoder: Decoder[T], schemaFor: SchemaFor[T]): KafkaDeserializer[T] = {
    val des = deserializer({ (topic, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val writerSchema = schemaRegistryClient.getById(schemaId)

      val reader = new GenericDatumReader[GenericRecord](writerSchema, schemaFor.schema)
      val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, new ByteBufferBackedInputStream(buffer))

      val readRecord = reader.read(null, jsonDecoder)

      decoder.decode(readRecord, schemaFor.schema)
    })
    
    if(includesFormatByte) {
      formatCheckingDeserializer(Format.AvroJsonSchemaId, des)  
    } else {
      des
    }
  }

  def avroJsonSchemaIdDeserializer[T: Decoder](schemaRegistryEndpoint: String,
                                                             isKey: Boolean,
                                                             includesFormatByte: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  def avroJsonSchemaIdDeserializer[T: Decoder](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                             isKey: Boolean,
                                                             includesFormatByte: Boolean): KafkaDeserializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey, includesFormatByte)
  }

  def avroJsonSchemaIdDeserializer[T](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean, includesFormatByte: Boolean)(implicit decoder: Decoder[T]): KafkaDeserializer[T] = {
    
    val des = deserializer({ (topic, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val writerSchema = schemaRegistryClient.getById(schemaId)

      val reader = new GenericDatumReader[GenericRecord](writerSchema, writerSchema)
      val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, new ByteBufferBackedInputStream(buffer))

      val readRecord = reader.read(null, jsonDecoder)

      decoder.decode(readRecord, writerSchema)
    })
    
    if(includesFormatByte) {
      formatCheckingDeserializer(Format.AvroJsonSchemaId, des)  
    } else {
      des
    }
    
  }

  def avroJsonSchemaIdSerializer[T: Encoder: SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean, includesFormatByte: Boolean): KafkaSerializer[T] =
    avroJsonSchemaIdSerializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey, includesFormatByte)

  def avroJsonSchemaIdSerializer[T: Encoder: SchemaFor](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                              isKey: Boolean, 
                                              includesFormatByte: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = initSchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey, includesFormatByte)
  }

  def avroJsonSchemaIdSerializer[T](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean, includesFormatByte: Boolean)(implicit encoder: Encoder[T], schemaFor: SchemaFor[T]): KafkaSerializer[T] = {
    val ser = serializer[T]{ (topic: String, t: T) =>
      val record = encoder.encode(t, schemaFor.schema).asInstanceOf[GenericRecord]
      val schemaId = schemaRegistryClient.register(s"$topic-${if (isKey) "key" else "value"}", record.getSchema)

      // TODO size hint
      val bout = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[GenericRecord](record.getSchema)
      val jsonEncoder = EncoderFactory.get.jsonEncoder(record.getSchema, bout)

      writer.write(record, jsonEncoder)

      bout.flush()
      bout.close()

      ByteBuffer.allocate(4).putInt(schemaId).array() ++ bout.toByteArray
    }

    if(includesFormatByte) {
      formatSerializer(Format.AvroJsonSchemaId,  ser)
    } else {
      ser
    }
  }
  
  private def initSchemaRegistryClient(settings: SchemaRegistryClientSettings): SchemaRegistryClient = {
    val config = settings.authentication match {
      case Authentication.Basic(username, password) =>
        Map(
          "basic.auth.credentials.source" -> "USER_INFO",
          "schema.registry.basic.auth.user.info" -> s"$username:$password"
        )
      case Authentication.None =>
        Map.empty[String, String]
    }

    new CachedSchemaRegistryClient(settings.endpoint, settings.maxCacheSize, config.asJava)
  }
}
