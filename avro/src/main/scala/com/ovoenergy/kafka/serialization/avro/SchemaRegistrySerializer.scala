package com.ovoenergy.kafka.serialization.avro

import io.confluent.kafka.serializers.KafkaAvroSerializer

private[avro] final class SchemaRegistrySerializer(client: JerseySchemaRegistryClient) extends KafkaAvroSerializer(client) {

  override def close(): Unit = {
    client.close()
    super.close()
  }

}

object SchemaRegistrySerializer {

  def apply(settings: SchemaRegistryClientSettings): KafkaAvroSerializer = {
    new SchemaRegistrySerializer(JerseySchemaRegistryClient(settings))
  }

}
