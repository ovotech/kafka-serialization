/*
 * Copyright 2017 OVO Energy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
