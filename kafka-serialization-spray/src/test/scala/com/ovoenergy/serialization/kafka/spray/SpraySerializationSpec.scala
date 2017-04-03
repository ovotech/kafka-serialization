package com.ovoenergy.serialization.kafka.spray

import java.nio.charset.StandardCharsets

import com.ovoenergy.serialization.kafka.core.Serialization.Format
import com.ovoenergy.serialization.kafka.spray.SpraySerialization._
import com.ovoenergy.serialization.kafka.testkit.UnitSpec

class SpraySerializationSpec extends UnitSpec with SpraySerialization {

  import spray.json._
  import DefaultJsonProtocol._
  import UnitSpec._

  implicit val eventFormat = jsonFormat2(Event)

  "SpraySerialization" when {
    "serializing" should {
      "put the Json format byte before the json body" in {

        val event = Event("123", "Foo")
        val serializer = spraySerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the json body after the format byte" in {

        val event = Event("123", "Foo")
        val serializer = spraySerializer[Event]

        val bytes = serializer.serialize("Does not matter", event)

        deserialize[Event](bytes.drop(1)) shouldBe event
      }
    }

    "deserializing" should {
      "raise an exception if the format is not Json" in {

        val event = Event("123", "Foo")
        val deserializer = sprayDeserializer[Event]

        an[Exception] should be thrownBy deserializer.deserialize("Does not matter", Array(90: Byte) ++ serialize(event))
      }

      "parse the json" in {

        val event = Event("123", "Foo")
        val deserializer = sprayDeserializer[Event]

        val bytes = Array(Format.toByte(Format.Json)) ++ serialize(event)

        deserializer.deserialize("Does not matter", bytes) shouldBe event
      }
    }
  }

}
