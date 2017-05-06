package com.ovoenergy.kafka.serialization.core

import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import scala.language.implicitConversions

trait Implicits {

  implicit def function2Serializer[T](f: (String, T) => Array[Byte]): KafkaSerializer[T] = serializer(f)

  implicit def function2Serializer[T](f: T => Array[Byte]): KafkaSerializer[T] = serializer(f)

  implicit def function2Deserializer[T](f: (String, Array[Byte]) => T): KafkaDeserializer[T] = deserializer(f)

  implicit def function2Deserializer[T](f: Array[Byte] => T): KafkaDeserializer[T] = deserializer(f)
}