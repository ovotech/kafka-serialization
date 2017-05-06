Kafka serialization/deserialization with format byte
====================================================

The aim of this library is to have a standard serialization/deserialization for kafka. It uses a byte in the head of the
serialized data to identify the serialization format. At the moment the supported formats are

 - JSON
 - Avro Binary with schema ID
 - Avro JSON with schema ID

The serializers/deserializers built by this library cannot be used in the Kafka configuration through properties, but need
to be passed through the Kafka Producer/Consumer constructors (It is not a bad thing IMHO).

For the Avro serialization this library uses Avro4s while for JSON it supports Json4s and Circe aut of the box. It is
easy to add support for other libraries.

## Modules

The library is composed by these modules:

 - kafka-serialization-core: provides the serialization primitives to build serializers and deserializers.
 - kafka-serialization-json4s: provides serializer and deserializer based on Json4s
 - kafka-serialization-spray: provides serializer and deserializer based on Spray Json
 - kafka-serialization-circe: provides serializer and deserializer based on Circe
 - kafka-serialization-avro: provides an improved schema-registry client based on Jersey 2.x
 - kafka-serialization-avro4s: provides serializer and deserializer based on Avro4s

The Avro4s serialization support the schema evolution through the schema registry. The consumer can provide its own schema
and Avro will take care of the conversion.

Simple deserialization example:

```scala
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.circe._
import com.ovoenergy.kafka.serialization.avro4s._
import com.ovoenergy.kafka.serialization.json4s._

import io.circe.generic.auto._
import io.circe.syntax._

case class Key(eventType: String)
case class Event(name: String)

val schemaRegistryEndpoint = "http:localhost:8081"

// Circe based deserializer
val keyDeserializer = circeJsonDeserializer[Key]

// Json4s based deserializer
val valueDeserializer = json4sDeserializer[Event]

// It will check the format byte and drop it
val checkingKeyDeserializer = formatCheckingDeserializer(Format.Json, keyDeserializer)

// It will drop the format byte without checking it
val droppingValueDeserializer = formatDroppingDeserializer(valueDeserializer)

// It will demultiplex the format byte and drop it
val formatDemultiplexer = formatDemultiplexerDeserializer[Event](notMatched => json4sDeserializer[Event]){
  case Format.AvroJsonSchemaId => avroJsonSchemaIdDeserializer[Event](schemaRegistryEndpoint)
  case Format.AvroBinarySchemaId => avroBinarySchemaIdDeserializer[Event](schemaRegistryEndpoint)
}

// It will demultiplex the topic
val topicDemultiplexer = topicDemultiplexerDeserializer[Event](notMatched => json4sDeserializer[Event]){
  case "topic-1" => avroJsonSchemaIdDeserializer[Event](schemaRegistryEndpoint)
  case "topic-2" => avroBinarySchemaIdDeserializer[Event](schemaRegistryEndpoint)
}

// All of these could be composed in a more complex configuration
```

