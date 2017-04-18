package com.ovoenergy.kafka.serialization.avro

import io.confluent.kafka.serializers.KafkaAvroDeserializer

private[avro] final class SchemaRegistryDeserializer(client: JerseySchemaRegistryClient) extends KafkaAvroDeserializer(client) {

  override def close(): Unit = {
    client.close()
    super.close()
  }

}

object SchemaRegistryDeserializer {

  def apply(settings: SchemaRegistryClientSettings): KafkaAvroDeserializer = {
    new SchemaRegistryDeserializer(JerseySchemaRegistryClient(settings))
  }

}
