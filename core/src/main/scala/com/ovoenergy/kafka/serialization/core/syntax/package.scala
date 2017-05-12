package com.ovoenergy.kafka.serialization.core

import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

package object syntax extends Implicits {

  implicit class RichFormat(val f: Format) extends AnyVal {
    def toByte: Byte = Format.toByte(f)
  }

  implicit class RichByte(val b: Byte) extends AnyVal {
    def toFormat: Format = Format.fromByte(b)
  }

  implicit class RichBytesToTFunction[T](f: Array[Byte] => T) {
    def asDeserializer: KafkaDeserializer[T] = deserializer(f)
  }

  implicit class RichStringAndBytesToTFunction[T](f: (String, Array[Byte]) => T) {
    def asDeserializer: KafkaDeserializer[T] = deserializer(f)
  }

  implicit class RichTToBytesFunction[T](f: T => Array[Byte]) {
    def asSerializer: KafkaSerializer[T] = serializer(f)
  }

  implicit class RichStringAndTToBytesFunction[T](f: (String, T) => Array[Byte]) {
    def asSerializer: KafkaSerializer[T] = serializer(f)
  }
}
