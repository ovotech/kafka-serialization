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
      "write the json body" in forAll { event: Event =>

        val serializer = spraySerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        new String(bytes, UTF_8).parseJson shouldBe event.toJson
      }
    }

    "deserializing" should {
      "parse the json" in forAll { event: Event =>

        val deserializer = sprayDeserializer[Event]

        val bytes = event.toJson.compactPrint.getBytes(UTF_8)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
