package com.ovoenergy.kafka.serialization.core

sealed trait Format

object Format {

  case object AvroBinarySchemaId extends Format

  case object AvroJsonSchemaId extends Format

  case object Json extends Format

  case class Custom(b: Byte) extends Format

  def toByte(f: Format): Byte = f match {
    case AvroBinarySchemaId => 0
    case AvroJsonSchemaId => 1
    case Json => 2
    case Custom(b) => b
  }

  def fromByte(b: Byte): Option[Format] = b match {
    case 0 => Some(AvroBinarySchemaId)
    case 1 => Some(AvroJsonSchemaId)
    case 2 => Some(Json)
    case n => Some(Custom(n))
  }

  implicit class RichFormat(val f: Format) extends AnyVal {
    def toByte: Byte = Format.toByte(f)
  }
}