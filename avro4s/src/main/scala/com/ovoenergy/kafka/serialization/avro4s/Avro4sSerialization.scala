package com.ovoenergy.kafka.serialization.avro4s

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.ovoenergy.kafka.serialization.avro.{JerseySchemaRegistryClient, SchemaRegistryClientSettings}
import com.ovoenergy.kafka.serialization.core._
import com.sksamuel.avro4s._
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
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
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdDeserializer(schemaRegistryClient, isKey, () => schemaRegistryClient.close(), includesFormatByte)
  }

  /**
    * Build a deserializer for binary-encoded messages prepended with a four byte Schema Registry schema ID.
    *
    * @param isKey true if this is a deserializer for keys, false if it is for values
    * @param includesFormatByte whether the messages are also prepended with a magic byte to specify the Avro format
    */
  def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                    isKey: Boolean,
                                                    includesFormatByte: Boolean): KafkaDeserializer[T] =
    avroBinarySchemaIdDeserializer(schemaRegistryClient, isKey, () => Unit, includesFormatByte)

  private def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                            isKey: Boolean,
                                                            close: () => Unit,
                                                            includesFormatByte: Boolean): KafkaDeserializer[T] = {

    val fromRecord = implicitly[FromRecord[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient)
    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    d.configure(Map("schema.registry.url" -> "").asJava, isKey)

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
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdWithReaderSchemaDeserializer(
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
    */
  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] =
    avroBinarySchemaIdWithReaderSchemaDeserializer(schemaRegistryClient, isKey, () => Unit, includesFormatByte)

  private def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord: SchemaFor](
    schemaRegistryClient: SchemaRegistryClient,
    isKey: Boolean,
    close: () => Unit,
    includesFormatByte: Boolean
  ): KafkaDeserializer[T] = {

    val fromRecord = implicitly[FromRecord[T]]
    val schemaFor = implicitly[SchemaFor[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient)
    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    d.configure(Map("schema.registry.url" -> "").asJava, isKey)

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

  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] =
    avroBinarySchemaIdSerializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryClientSettings: SchemaRegistryClientSettings,
                                                isKey: Boolean): KafkaSerializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroBinarySchemaIdSerializer(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
  }

  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                isKey: Boolean): KafkaSerializer[T] =
    avroBinarySchemaIdSerializer(schemaRegistryClient, isKey, () => Unit)

  private def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient,
                                                        isKey: Boolean,
                                                        close: () => Unit): KafkaSerializer[T] = {

    val toRecord = implicitly[ToRecord[T]]
    val kafkaAvroSerializer = new KafkaAvroSerializer(schemaRegistryClient)
    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    kafkaAvroSerializer.configure(Map("schema.registry.url" -> "").asJava, isKey)

    serializer({ (topic, t) =>
      val bytes = kafkaAvroSerializer.serialize(topic, toRecord(t))
      if (bytes != null && bytes.nonEmpty) {
        bytes.drop(1)
      } else {
        bytes
      }
    }, close)
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryEndpoint: String,
                                                                  isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                                  isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey, () => Unit)

  private def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryClient: SchemaRegistryClient,
                                                                          isKey: Boolean,
                                                                          close: () => Unit): KafkaDeserializer[T] =
    formatCheckingDeserializer(Format.AvroJsonSchemaId, deserializer({ (topic, data) =>
      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val schema = schemaRegistryClient.getByID(schemaId)

      implicit val schemaFor: SchemaFor[T] = new SchemaFor[T] {
        override def apply(): Schema = schema
      }

      val avroIn = AvroJsonInputStream[T](new ByteBufferBackedInputStream(buffer))

      avroIn.singleEntity.get
    }, close))

  def avroJsonSchemaIdDeserializer[T: FromRecord: SchemaFor](schemaRegistryEndpoint: String,
                                                             isKey: Boolean): KafkaDeserializer[T] =
    avroJsonSchemaIdDeserializer(SchemaRegistryClientSettings(schemaRegistryEndpoint), isKey)

  def avroJsonSchemaIdDeserializer[T: FromRecord: SchemaFor](
    schemaRegistryClientSettings: SchemaRegistryClientSettings,
    isKey: Boolean
  ): KafkaDeserializer[T] = {
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
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
      val writerSchema = schemaRegistryClient.getByID(schemaId)
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
    val schemaRegistryClient = JerseySchemaRegistryClient(schemaRegistryClientSettings)
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey, () => schemaRegistryClient.close())
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
}
