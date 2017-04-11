package com.ovoenergy.serialization.kafka.avro4s

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.ovoenergy.kafka.serialization.avro.{JerseySchemaRegistryClient, SchemaRegistryClientSettings}
import com.ovoenergy.serialization.kafka.core.Serialization._
import com.sksamuel.avro4s._
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

object Avro4sSerialization {

  def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
    avroBinarySchemaIdDeserializer(JerseySchemaRegistryClient(SchemaRegistryClientSettings(schemaRegistryEndpoint)), isKey)
  }

  def avroBinarySchemaIdDeserializer[T: FromRecord](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean): KafkaDeserializer[T] = {
    import scala.collection.JavaConverters._

    val fromRecord = implicitly[FromRecord[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient)
    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    d.configure(Map("schema.registry.url" -> "").asJava, isKey)

    deserializer({ (topic, data) =>
      fromRecord(d.deserialize(topic, data).asInstanceOf[GenericRecord])
    })
  }

  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord : SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] =
    avroBinarySchemaIdWithReaderSchemaDeserializer(JerseySchemaRegistryClient(SchemaRegistryClientSettings(schemaRegistryEndpoint)), isKey)

  def avroBinarySchemaIdWithReaderSchemaDeserializer[T: FromRecord : SchemaFor](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean): KafkaDeserializer[T] = {
    import scala.collection.JavaConverters._

    val fromRecord = implicitly[FromRecord[T]]
    val schemaFor = implicitly[SchemaFor[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient)
    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    d.configure(Map("schema.registry.url" -> "").asJava, isKey)

    deserializer { (topic, data) =>
      fromRecord(d.deserialize(topic, data, schemaFor()).asInstanceOf[GenericRecord])
    }
  }

  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] = {
    avroBinarySchemaIdSerializer(JerseySchemaRegistryClient(SchemaRegistryClientSettings(schemaRegistryEndpoint)), isKey)
  }

  def avroBinarySchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean): KafkaSerializer[T] = {
    import scala.collection.JavaConverters._

    val toRecord = implicitly[ToRecord[T]]
    val kafkaAvroSerializer = new KafkaAvroSerializer(schemaRegistryClient)
    // The configure method needs the `schema.registry.url` even if the schema registry client has been provided
    kafkaAvroSerializer.configure(Map("schema.registry.url" -> "").asJava, isKey)

    serializer { (topic, t) =>
      kafkaAvroSerializer.serialize(topic, toRecord(t))
    }
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {

    val schemaRegistryClient = new JerseySchemaRegistryClient(SchemaRegistryClientSettings(schemaRegistryEndpoint))
    avroJsonSchemaIdDeserializerWithReaderSchema(schemaRegistryClient, isKey)
  }

  def avroJsonSchemaIdDeserializerWithReaderSchema[T: FromRecord](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean): KafkaDeserializer[T] = {

    formatCheckingDeserializer(Format.AvroJsonSchemaId, deserializer { (topic, data) =>

      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val schema = schemaRegistryClient.getByID(schemaId)

      implicit val schemaFor: SchemaFor[T] = new SchemaFor[T] {
        override def apply() = schema
      }

      val avroIn = AvroJsonInputStream[T](new ByteBufferBackedInputStream(buffer))

      avroIn.singleEntity.get
    })

  }

  def avroJsonSchemaIdDeserializer[T: FromRecord : SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {

    val schemaRegistryClient = new JerseySchemaRegistryClient(SchemaRegistryClientSettings(schemaRegistryEndpoint))
    avroJsonSchemaIdDeserializer(schemaRegistryClient, isKey)
  }

  def avroJsonSchemaIdDeserializer[T: FromRecord : SchemaFor](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean): KafkaDeserializer[T] = {

    formatCheckingDeserializer(Format.AvroJsonSchemaId, deserializer { (topic, data) =>

      val buffer = ByteBuffer.wrap(data)
      val schemaId = buffer.getInt
      val writerSchema = schemaRegistryClient.getByID(schemaId)
      val readerSchema = implicitly[SchemaFor[T]].apply()

      val reader = new GenericDatumReader[GenericRecord](writerSchema, readerSchema)
      val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, new ByteBufferBackedInputStream(buffer))

      val readRecord = reader.read(null, jsonDecoder)

      implicitly[FromRecord[T]].apply(readRecord)
    })
  }

  def avroJsonSchemaIdSerializer[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] = {

    val schemaRegistryClient = new JerseySchemaRegistryClient(SchemaRegistryClientSettings(schemaRegistryEndpoint))
    avroJsonSchemaIdSerializer(schemaRegistryClient, isKey)
  }

  def avroJsonSchemaIdSerializer[T: ToRecord](schemaRegistryClient: SchemaRegistryClient, isKey: Boolean): KafkaSerializer[T] = {

    val toRecord: ToRecord[T] = implicitly

    formatSerializer(Format.AvroJsonSchemaId, serializer { (topic, t) =>

      val record = toRecord(t)
      implicit val schemaFor: SchemaFor[T] = new SchemaFor[T] {
        override def apply() = record.getSchema
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
    })
  }
}
