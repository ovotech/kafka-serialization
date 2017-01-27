Kafka serialization with format byte
====================================
The aim of this library is to have a standard serialization/deserialization for kafka. It uses a byte in the head of the
serialized data to identify the serialization format. At the moment the supported format are JSON, Avro Binary with 
schema Id, Avro JSON with schema ID.

The serializers/deserializer built by this library cannot be used in the Kafka configuration trough properties, but need
to be passed trough the Kafka Producer/Consumer constructors (It is not a bad think IMHO).

For the Avro serialization this library uses Avro4s while for JSON it supports Json4s and Circe aut of the box. It is
easy to add support for other libraries.

## Modules
The library is composed by these modules:
 - Serialization: provides the serialization primitives to build serializers and deserializers.
 - Json4sSerialization: provides serializer and deserializer based on Json4s
 - CirceSerialization: provides serializer and deserializer based on Circe
 - Avro4sSerialization: provides serializer and deserializer based on Avro4s
 
The Avro4s serialization support the schema evolution trough the schema registry. The consumer can provide its own schema
and Avro will take care of the conversion.

Simple Circe serialization example:
````scala
import io.circe.generic.auto._
import io.circe.jawn.JawnParser
import io.circe.syntax._
import java.nio.charset.StandardCharsets._
import com.ovoenergy.kafka.serialization.CirceSerialization._
import com.ovoenergy.kafka.serialization.Serialization._
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.Properties

case class Key(eventType: String)
case class Event(name: String)

val props = new Properties()
val producer = new KafkaProducer[Key, Event](props, circeJsonSerializer[Key], circeJsonSerializer[Event])
val consumer = new KafkaConsumer[Key, Event](props, circeJsonDeserializer[Key], circeJsonDeserializer[Event])

// Non strict consumer
val nonStrictConsumer = new KafkaConsumer[Key, () => Event](props, circeJsonDeserializer[Key], nonStrictDeserializer(circeJsonDeserializer[Event]))
````

The deserializer is able to demultiplex on different format:
````scala
import io.circe.generic.auto._
import io.circe.jawn.JawnParser
import io.circe.syntax._
import java.nio.charset.StandardCharsets._
import com.ovoenergy.kafka.serialization.CirceSerialization._
import com.ovoenergy.kafka.serialization.Serialization._
import com.ovoenergy.kafka.serialization.Avro4sSerialization._
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.Properties
import com.sksamuel.avro4s._

case class Key(eventType: String)
case class Event(name: String)

implicit val eventFromRecord = FromRecord[Event]

val schemaRegistryEndpoint: String  = "http://localhost:8080"
val props = new Properties()

val producerOne = new KafkaProducer[Key, Event](props, circeJsonSerializer[Key], circeJsonSerializer[Event])
val producerTwo = new KafkaProducer[Key, Event](props, circeJsonSerializer[Key], avroBinarySchemaIdSerializer[Event](schemaRegistryEndpoint, isKey = false))

// Non strict consumer
val consumer = new KafkaConsumer[Key, Event](props, circeJsonDeserializer[Key], formatDemultiplexerDeserializer(
  Format.AvroBinarySchemaId -> avroBinarySchemaIdDeserializer(schemaRegistryEndpoint, isKey = false),
  Format.Json -> circeJsonDeserializer[Event]
))
````

## About this README

The code samples in this README file are checked using [tut](https://github.com/tpolecat/tut).

This means that the `README.md` file is generated from `src/main/tut/README.md`. If you want to make any changes to the README, you should:

1. Edit `src/main/tut/README.md`
2. Run `sbt tut` to regenerate `./README.md`
3. Commit both files to git
