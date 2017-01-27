package com.ovoenergy.kafka.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.ovoenergy.kafka.serialization.Serialization._
import com.sksamuel.avro4s._
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

object Avro4sSerialization {

  def deserializeAvroBinaryAndSchemaRegistry[T: FromRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
    import scala.collection.JavaConverters._

    val fromRecord = implicitly[FromRecord[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer
    d.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

    deserializer({ (topic, data) =>
      fromRecord(d.deserialize(topic, data).asInstanceOf[GenericRecord])
    })
  }

  def deserializeAvroBinaryAndSchemaRegistryWithReaderSchema[T: FromRecord : SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
    import scala.collection.JavaConverters._

    val fromRecord = implicitly[FromRecord[T]]
    val schemaFor = implicitly[SchemaFor[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer
    d.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

    deserializer { (topic, data) =>
      fromRecord(d.deserialize(topic, data, schemaFor()).asInstanceOf[GenericRecord])
    }
  }

  def serializeWithAvroBinaryAndSchemaRegistry[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] = {
    import scala.collection.JavaConverters._

    val toRecord = implicitly[ToRecord[T]]
    val kafkaAvroSerializer = new KafkaAvroSerializer()
    kafkaAvroSerializer.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

    serializer{ (topic, t) =>
      kafkaAvroSerializer.serialize(topic, toRecord(t))
    }
  }

  def avroJsonAndSchemaRegistryDeserializer[T : FromRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {

    val schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryEndpoint, 1000)

    deserializerWithFormatCheck(Format.AvroJsonWithSchema, deserializer{ (topic, data) =>

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

  def avroJsonAndSchemaRegistryWithReaderSchemaDeserializer[T : FromRecord : SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {

    val schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryEndpoint, 1000)

    deserializerWithFormatCheck(Format.AvroJsonWithSchema, deserializer{ (topic, data) =>

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

  def avroJsonAndSchemaRegistrySerializer[T: ToRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaSerializer[T] = {

    val schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryEndpoint, 1000)
    val toRecord: ToRecord[T] = implicitly

    serializerWithMagicByte(Format.AvroBinaryWithSchema, serializer{ (topic, t) =>

      val record = toRecord(t)
      implicit val schemaFor: SchemaFor[T] = new SchemaFor[T] {
        override def apply() = record.getSchema
      }

      val schemaId = schemaRegistryClient.register(s"$topic-${if(isKey) "key" else "value" }", record.getSchema)

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
