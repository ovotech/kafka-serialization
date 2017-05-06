package com.ovoenergy.kafka.serialization.spray

import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core.Format
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import StandardCharsets._
class SpraySerializationSpec extends UnitSpec with SpraySerialization {

  import spray.json._
  import DefaultJsonProtocol._
  import com.ovoenergy.kafka.serialization.testkit.UnitSpec._

  implicit val eventFormat: RootJsonFormat[Event] = jsonFormat2(Event)

  "SpraySerialization" when {
    "serializing" should {
      "put the Json format byte before the json body" in forAll { event: Event =>

        val serializer = spraySerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the json body after the format byte" in forAll { event: Event =>

        val serializer = spraySerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        new String(bytes.drop(1), UTF_8).parseJson shouldBe event.toJson
      }
    }

    "deserializing" should {
      "raise an exception if the format is not Json" in forAll { event: Event =>

        val deserializer = sprayDeserializer[Event]

        an[Exception] should be thrownBy deserializer.deserialize("Does not matter", Array(90: Byte) ++ event.toJson.compactPrint.getBytes(UTF_8))
      }

      "parse the json" in forAll { event: Event =>

        val deserializer = sprayDeserializer[Event]

        val bytes = Array(Format.toByte(Format.Json)) ++ event.toJson.compactPrint.getBytes(UTF_8)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
