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

/**
  * Represent a payload format.
  *
  * It will be serialized as a single byte as payload prefix. Different formats have different byte value.
  */
sealed trait Format

object Format {

  /**
    * This is an Avro binary message prefixed by 4 bytes containing a schema id obtained fro mthe schema registry.
    */
  case object AvroBinarySchemaId extends Format

  /**
    * This is an Avro JSON message prefixed by 4 bytes containing a schema id obtained fro mthe schema registry.
    */
  case object AvroJsonSchemaId extends Format

  /**
    * This is JSON message.
    */
  case object Json extends Format

  /**
    * This is custom format message where the byte value is given in the constructor.
    */
  case class Custom(b: Byte) extends Format

  def toByte(f: Format): Byte = f match {
    case AvroBinarySchemaId => 0
    case AvroJsonSchemaId   => 1
    case Json               => 2
    case Custom(b)          => b
  }

  def fromByte(b: Byte): Format = b match {
    case 0 => AvroBinarySchemaId
    case 1 => AvroJsonSchemaId
    case 2 => Json
    case n => Custom(n)
  }
}
