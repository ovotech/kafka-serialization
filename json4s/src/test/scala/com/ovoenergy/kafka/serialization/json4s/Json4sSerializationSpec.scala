package com.ovoenergy.kafka.serialization.json4s

import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core.Format
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import org.json4s.DefaultFormats
import org.json4s.native.{Serialization => JsonSerialization}

class Json4sSerializationSpec extends UnitSpec with Json4sSerialization{

  import StandardCharsets.UTF_8

  import JsonSerialization._
  import com.ovoenergy.kafka.serialization.testkit.UnitSpec._

  implicit val formats = DefaultFormats

  "Json4sSerialization" when {
    "serializing" should {
      "put the Json format byte before the json body" in forAll { event: Event =>

        val serializer = json4sSerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the Json json body after the format byte" in forAll { event: Event =>

        val serializer = json4sSerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        read[Event](new String(bytes.drop(1), UTF_8)) shouldBe event
      }
    }

    "deserializing" should {
      "raise an exception if the format is not Json" in forAll { event: Event =>

        val deserializer = json4sDeserializer[Event]

        an[Exception] should be thrownBy deserializer.deserialize("Does not matter", Array(90: Byte) ++ write(event).getBytes(UTF_8))
      }

      "parse the json" in forAll { event: Event =>

        val deserializer = json4sDeserializer[Event]

        val bytes = Array(Format.toByte(Format.Json)) ++ write(event).getBytes(UTF_8)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
