package com.ovoenergy.serialization.kafka.json4s

import java.nio.charset.StandardCharsets

import com.ovoenergy.serialization.kafka.core.Serialization.Format
import com.ovoenergy.serialization.kafka.testkit.UnitSpec
import org.json4s.DefaultFormats
import org.json4s.native.{Serialization => JsonSerialization}

class Json4sSerializationSpec extends UnitSpec {

  import StandardCharsets.UTF_8

  import Json4sSerialization._
  import JsonSerialization._
  import UnitSpec._

  implicit val formats = DefaultFormats

  "Json4sSerialization" when {
    "serializing" should {
      "put the Json format byte before the json body" in {

        val event = Event("123", "Foo")
        val serializer = json4sSerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the Json json body after the format byte" in {

        val event = Event("123", "Foo")
        val serializer = json4sSerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        read[Event](new String(bytes.drop(1), UTF_8)) shouldBe event
      }
    }

    "deserializing" should {
      "raise an exception if the format is not Json" in {

        val event = Event("123", "Foo")
        val deserializer = json4sDeserializer[Event]

        an[Exception] should be thrownBy deserializer.deserialize("Does not matter", Array(90: Byte) ++ write(event).getBytes(UTF_8))
      }

      "parse the json" in {

        val event = Event("123", "Foo")
        val deserializer = json4sDeserializer[Event]

        val bytes = Array(Format.toByte(Format.Json)) ++ write(event).getBytes(UTF_8)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
