package com.ovoenergy.kafka.serialization.avro.okhttp
import com.ovoenergy.kafka.serialization.avro.{SchemaRegistryClientSettings, SchemaRegistryClientSpec}
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient

class OkHttpSchemaRegistryClientSpec extends SchemaRegistryClientSpec {

  override def implementationName: String = "OkHttpSchemaRegistryClient"

  override def withSchemaRegistryClient[T](settings: SchemaRegistryClientSettings)(f: SchemaRegistryClient => T): T =
    resource.managed(new OkHttpSchemaRegistryClient(settings)).acquireAndGet(f)
}
