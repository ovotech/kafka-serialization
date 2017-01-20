package com.ovoenergy.kafka.serialization

import com.ovoenergy.kafka.serialization.Serialization._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

object Avro4sSerialization {

  // TODO define ToRecord and SchemaFor Record.Envelope

  def deserializeAvroBinaryAndSchemaRegistry[T: FromRecord](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
    import scala.collection.JavaConverters._

    val fromRecord = implicitly[FromRecord[T]]

    val d: KafkaAvroDeserializer = new KafkaAvroDeserializer
    d.configure(Map("schema.registry.url" -> schemaRegistryEndpoint).asJava, isKey)

    deserializer({ (topic, data) =>
      fromRecord(d.deserialize(topic, data).asInstanceOf[GenericRecord])
    })
  }

  def deserializeAvroBinaryAndSchemaRegistry[T: FromRecord : SchemaFor](schemaRegistryEndpoint: String, isKey: Boolean): KafkaDeserializer[T] = {
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

}
