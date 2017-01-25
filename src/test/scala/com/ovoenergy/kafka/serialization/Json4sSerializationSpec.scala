package com.ovoenergy.kafka.serialization

import java.io.ByteArrayInputStream

import com.ovoenergy.UnitSpec
import com.ovoenergy.kafka.serialization.Serialization.Format
import org.json4s.{DefaultFormats, StreamInput}
import org.json4s.native.{Serialization => JsonSerialization, _}
import org.json4s.Extraction._
import java.nio.charset.StandardCharsets
import scala.util.Random

class Json4sSerializationSpec extends UnitSpec {
  import UnitSpec._
  import Json4sSerialization._
  import JsonSerialization._
  import StandardCharsets.UTF_8

  implicit val formats = DefaultFormats

  "Json4sSerialization" when {
    "serializing" should {
      "put the Json format byte before the json body" in {

        val event = Event("123", "Foo")
        val serializer = serializeWithJson4sJson[Event]

        val bytes = serializer.serialize("Does not matter", event)

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the Json json body after the format byte" in {

        val event = Event("123", "Foo")
        val serializer = serializeWithJson4sJson[Event]

        val bytes = serializer.serialize("Does not matter", event)

        read[Event](new String(bytes.drop(1), UTF_8)) shouldBe event
      }
    }

    "deserializing" should {
      "raise an exception if the format is not Json" in {

        val event = Event("123", "Foo")
        val deserializer = deserializeWithJson4s[Event]

        an[Exception] should be thrownBy deserializer.deserialize("Does not matter", Array(120: Byte) ++ Array.fill(100)(Random.nextInt().toByte))
      }

      "parse the json" in {

        val event = Event("123", "Foo")
        val deserializer = deserializeWithJson4s[Event]

        val bytes = Array(Format.toByte(Format.Json)) ++ write(event).getBytes(UTF_8)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
