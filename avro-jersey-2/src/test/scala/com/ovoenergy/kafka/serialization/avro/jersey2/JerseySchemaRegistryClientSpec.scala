package com.ovoenergy.kafka.serialization.avro.jersey2

import com.ovoenergy.kafka.serialization.avro.{SchemaRegistryClientSettings, SchemaRegistryClientSpec}
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient

class JerseySchemaRegistryClientSpec extends SchemaRegistryClientSpec {

  override def implementationName: String = "JerseySchemaRegistryClient"

  override def withSchemaRegistryClient[T](settings: SchemaRegistryClientSettings)(f: SchemaRegistryClient => T): T =
    resource.managed(new JerseySchemaRegistryClient(settings)).acquireAndGet(f)
}
