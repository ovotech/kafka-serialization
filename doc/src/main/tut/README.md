# Kafka serialization/deserialization building blocks

[![CircleCI Badge](https://circleci.com/gh/ovotech/kafka-serialization.svg?style=shield)](https://circleci.com/gh/ovotech/kafka-serialization)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a2d814f22d4e4facae0f8a3eb1c841fd)](https://www.codacy.com/app/filippo-deluca/kafka-serialization?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ovotech/kafka-serialization&amp;utm_campaign=Badge_Grade)
[![Download](https://api.bintray.com/packages/ovotech/maven/kafka-serialization/images/download.svg)](https://bintray.com/ovotech/maven/kafka-serialization/_latestVersion)

The aim of this library is to provide the Lego&trade; bricks to build a serializer/deserializer for kafka messages. 

The serializers/deserializers built by this library cannot be used in the Kafka configuration through properties, but 
need to be passed through the Kafka Producer/Consumer constructors (It is feature IMHO).

For the Avro serialization this library uses Avro4s while for JSON it supports Json4s, Circe and Spray out of the box. 
It is quite easy to add support for other libraries as well.

## Modules

The library is composed by these modules:

- kafka-serialization-core: provides the serialization primitives to build serializers and deserializers.
- kafka-serialization-cats: provides cats typeclasses instances for serializers and deserializers.
- kafka-serialization-json4s: provides serializer and deserializer based on Json4s
- kafka-serialization-jsoniter-scala: provides serializer and deserializer based on Jsoniter Scala
- kafka-serialization-spray: provides serializer and deserializer based on Spray Json
- kafka-serialization-circe: provides serializer and deserializer based on Circe
- kafka-serialization-avro: provides an schema-registry client settings
- kafka-serialization-avro4s: provides serializer and deserializer based on Avro4s 1.x
- kafka-serialization-avro4s2: provides serializer and deserializer based on Avro4s 2.x

The Avro4s serialization support the schema evolution through the schema registry. The consumer can provide its own schema
and Avro will take care of the conversion.

## Getting Started

The library is available in the Bintray OVO repository. Add this snippet to your build.sbt to use it.

```sbtshell
import sbt._
import sbt.Keys.

resolvers += Resolver.bintrayRepo("ovotech", "maven")

libraryDependencies ++= {
  val kafkaSerializationV = "0.1.23" // see the Maven badge above for the latest version
  Seq(
    "com.ovoenergy" %% "kafka-serialization-core" % kafkaSerializationV,
    "com.ovoenergy" %% "kafka-serialization-circe" % kafkaSerializationV, // To provide Circe JSON support
    "com.ovoenergy" %% "kafka-serialization-json4s" % kafkaSerializationV, // To provide Json4s JSON support
    "com.ovoenergy" %% "kafka-serialization-jsoniter-scala" % kafkaSerializationV, // To provide Jsoniter Scala JSON support
    "com.ovoenergy" %% "kafka-serialization-spray" % kafkaSerializationV, // To provide Spray-json JSON support
    "com.ovoenergy" %% "kafka-serialization-avro4s" % kafkaSerializationV // To provide Avro4s Avro support
  )
}

```

## Circe example

Circe is a JSON library for Scala that provides support for generic programming trough Shapeless. You can find more 
information on the [Circe website](https://circe.github.io/circe).

Simple serialization/deserialization example with Circe:

```tut:silent
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.circe._

// Import the Circe generic support
import io.circe.generic.auto._
import io.circe.syntax._

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.CommonClientConfigs._

import scala.collection.JavaConverters._

case class UserCreated(id: String, name: String, age: Int)

val producer = new KafkaProducer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava, 
  nullSerializer[Unit], 
  circeJsonSerializer[UserCreated]
)

val consumer = new KafkaConsumer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava,
  nullDeserializer[Unit],
  circeJsonDeserializer[UserCreated]
)
```

```tut:invisible
producer.close()
consumer.close()
```

## Jsoniter Scala example

[Jsoniter Scala](https://github.com/plokhotnyuk/jsoniter-scala). is a library that generates codecs for case classes, 
standard types and collections to get maximum performance of JSON parsing & serialization.

Here is an example of serialization/deserialization with Jsoniter Scala:

```tut:silent
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.jsoniter_scala._

// Import the Jsoniter Scala macros & core support
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.CommonClientConfigs._

import scala.collection.JavaConverters._

case class UserCreated(id: String, name: String, age: Int)

implicit val userCreatedCodec: JsonValueCodec[UserCreated] = JsonCodecMaker.make[UserCreated](CodecMakerConfig())

val producer = new KafkaProducer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava, 
  nullSerializer[Unit],
  jsoniterScalaSerializer[UserCreated]()
)

val consumer = new KafkaConsumer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava,
  nullDeserializer[Unit],
  jsoniterScalaDeserializer[UserCreated]()
)
```

```tut:invisible
producer.close()
consumer.close()
```

## Avro example

Apache Avro is a remote procedure call and data serialization framework developed within Apache's Hadoop project. It uses 
JSON for defining data types and protocols, and serializes data in a compact binary format.

Apache Avro provide some support to evolve your messages across multiple version without breaking compatibility with 
older or newer consumers. It supports several encoding formats but two are the most used in Kafka: Binary and Json.

The encoded data is always validated and parsed using a Schema (defined in JSON) and eventually evolved to the reader 
Schema version.

This library provided the support to Avro by using the [Avro4s](https://github.com/sksamuel/avro4s) libray. It uses macro
and shapeless to allowing effortless serialization and deserialization. In addition to Avro4s it need a Confluent schema
registry in place, It will provide a way to control the format of the messages produced in kafka. You can find more 
information in the [Confluent Schema Registry Documentation ](http://docs.confluent.io/current/schema-registry/docs/).


An example with Avro4s binary and Schema Registry:
```tut:silent
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.avro4s._

import com.sksamuel.avro4s._

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.CommonClientConfigs._

import scala.collection.JavaConverters._

val schemaRegistryEndpoint = "http://localhost:8081"

case class UserCreated(id: String, name: String, age: Int)

// This type class is need by the avroBinarySchemaIdSerializer
implicit val UserCreatedToRecord = ToRecord[UserCreated]

val producer = new KafkaProducer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava, 
  nullSerializer[Unit], 
  avroBinarySchemaIdSerializer[UserCreated](schemaRegistryEndpoint, isKey = false, includesFormatByte = true)
)

// This type class is need by the avroBinarySchemaIdDeserializer
implicit val UserCreatedFromRecord = FromRecord[UserCreated]

val consumer = new KafkaConsumer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava,
  nullDeserializer[Unit],
  avroBinarySchemaIdDeserializer[UserCreated](schemaRegistryEndpoint, isKey = false, includesFormatByte = true)
)
```

```tut:invisible
producer.close()
consumer.close()
```

This Avro serializer will try to register the schema every new message type it will serialize and will save the obtained 
schema id in cache. The deserializer will contact the schema registry each time it will encounter a message with a never
seen before schema id. 

The schema id will encoded in the first 4 bytes of the payload. The deserializer will extract the schema id from the 
payload and fetch the schema from the schema registry. The deserializer is able to evolve the original message to the 
consumer schema. The use case is when the consumer is only interested in a part of the original message (schema projection) 
or when the original message is in a older or newer format of the cosumer schema (schema evolution).

An example of the consumer schema:
```tut:silent
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.avro4s._

import com.sksamuel.avro4s._

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.CommonClientConfigs._

import scala.collection.JavaConverters._

val schemaRegistryEndpoint = "http://localhost:8081"

/* Assuming the original message has been serialized using the 
 * previously defined UserCreated class. We are going to project
 * it ignoring the value of the age
 */
case class UserCreated(id: String, name: String)

// This type class is need by the avroBinarySchemaIdDeserializer
implicit val UserCreatedFromRecord = FromRecord[UserCreated]


/* This type class is need by the avroBinarySchemaIdDeserializer 
 * to obtain the consumer schema
 */
implicit val UserCreatedSchemaFor = SchemaFor[UserCreated]

val consumer = new KafkaConsumer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava,
  nullDeserializer[Unit],
  avroBinarySchemaIdWithReaderSchemaDeserializer[UserCreated](schemaRegistryEndpoint, isKey = false, includesFormatByte = false)
)
```

```tut:invisible
consumer.close()
```

## Format byte

The Original Confluent Avro serializer/deserializer prefix the payload with a "magic" byte to identify that the message 
has been written with the Avro serializer. 

Similarly this library support the same mechanism by mean of a couple of function. It is even able to multiplex and 
demultiplex different serializers/deserializers based on that format byte. At the moment the supported formats are
  - JSON
  - Avro Binary with schema ID
  - Avro JSON with schema ID

let's see this mechanism in action:
```tut:silent
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.avro4s._
import com.ovoenergy.kafka.serialization.circe._

// Import the Circe generic support
import io.circe.generic.auto._
import io.circe.syntax._

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.CommonClientConfigs._


sealed trait Event
case class UserCreated(id: String, name: String, email: String) extends Event

/* This producer will produce messages in Avro binary format */
val avroBinaryProducer = new KafkaProducer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava, 
  nullSerializer[Unit],   
  formatSerializer(Format.AvroBinarySchemaId, avroBinarySchemaIdSerializer[UserCreated](schemaRegistryEndpoint, isKey = false, includesFormatByte = false))
)

/* This producer will produce messages in Json format */
val circeProducer = new KafkaProducer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava, 
  nullSerializer[Unit],   
  formatSerializer(Format.Json, circeJsonSerializer[UserCreated])
)

/* This consumer will be able to consume messages from both producer */
val consumer = new KafkaConsumer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava,
  nullDeserializer[Unit],
  formatDemultiplexerDeserializer[UserCreated](unknownFormat => failingDeserializer(new RuntimeException("Unsupported format"))){
    case Format.Json => circeJsonDeserializer[UserCreated]
    case Format.AvroBinarySchemaId => avroBinarySchemaIdDeserializer[UserCreated](schemaRegistryEndpoint, isKey = false, includesFormatByte = false)
  }
)

/* This consumer will be able to consume messages in Avro binary format with the magic format byte at the start */
val avroBinaryConsumer = new KafkaConsumer(
  Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG->"localhost:9092").asJava,
  nullDeserializer[Unit],
  avroBinarySchemaIdDeserializer[UserCreated](schemaRegistryEndpoint, isKey = false, includesFormatByte = true)
)
```

```tut:invisible
avroBinaryProducer.close()
circeProducer.close()
consumer.close()
avroBinaryConsumer.close()
```

You can notice that the `formatDemultiplexerDeserializer` is little bit nasty because it is invariant in the type `T` so
all the demultiplexed `serialiazer` must be declared as `Deserializer[T]`.

There are other support serializer and deserializer, you can discover them looking trough the code and the tests.

## Useful de-serializers

In the core module there are pleanty of serializers and deserializers that handle generic cases.

### Optional deserializer

To handle the case in which the data is null, you need to wrap the deserializer in the `optionalDeserializer`:

```tut:silent
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.circe._

// Import the Circe generic support
import io.circe.generic.auto._
import io.circe.syntax._

import org.apache.kafka.common.serialization.Deserializer

case class UserCreated(id: String, name: String, age: Int)

val userCreatedDeserializer: Deserializer[Option[UserCreated]] = optionalDeserializer(circeJsonDeserializer[UserCreated])
```

## Cats instances

The `cats` module provides the `Functor` typeclass instance for the `Deserializer` and `Contravariant` instance for the 
`Serializer`. This allow to do:

```tut:silent
import cats.implicits._
import com.ovoenergy.kafka.serialization.core._
import com.ovoenergy.kafka.serialization.cats._
import org.apache.kafka.common.serialization.{Serializer, Deserializer, IntegerSerializer, IntegerDeserializer}

val intDeserializer: Deserializer[Int] = (new IntegerDeserializer).asInstanceOf[Deserializer[Int]]
val stringDeserializer: Deserializer[String] = intDeserializer.map(_.toString)
 
val intSerializer: Serializer[Int] = (new IntegerSerializer).asInstanceOf[Serializer[Int]]
val stringSerializer: Serializer[String] = intSerializer.contramap(_.toInt)
```

## Complaints and other Feedback

Feedback of any kind is always appreciated.

Issues and PR's are welcome as well.

## About this README

The code samples in this README file are checked using [tut](https://github.com/tpolecat/tut).

This means that the `README.md` file is generated from `doc/src/main/tut/README.md`. If you want to make any changes to the README, you should:

1. Edit `doc/src/main/tut/README.md`
2. Run `sbt tut` to regenerate `./README.md`
3. Commit both files to git