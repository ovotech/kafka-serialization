package com.ovoenergy.serialization.kafka.circe

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.ovoenergy.serialization.kafka.core.Serialization.Format
import com.ovoenergy.serialization.kafka.testkit.UnitSpec
import io.circe.generic.auto._
import io.circe.jawn.JawnParser
import io.circe.syntax._

class CirceSerializationSpec extends UnitSpec {

  import StandardCharsets._

  import UnitSpec._
  import com.ovoenergy.serialization.kafka.circe.CirceSerialization._

  "CirceSerialization" when {
    "serializing" should {
      "put the Json format byte before the body" in {

        val serializer = circeJsonSerializer[Event]
        val bytes = serializer.serialize("Does not matter", Event("123", "MyEvent"))

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the Json body after the format byte" in {

        val serializer = circeJsonSerializer[Event]
        val event = Event("123", "MyEvent")
        val bytes = serializer.serialize("Does not matter", event)

        new JawnParser().parseByteBuffer(ByteBuffer.wrap(bytes.drop(1))) shouldBe Right(event.asJson)
      }
    }

    "deserializing" should {
      "raise an error if the format byte does not match" in {

        val event = Event("123", "MyEvent")
        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializer[Event]

        an[Exception] should be thrownBy deserializer.deserialize("does not matter", Array(90: Byte) ++ jsonBytes)
      }

      "parse the json after the format byte" in {

        val event = Event("123", "MyEvent")
        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializer[Event]

        val deserialized = deserializer.deserialize("does not matter", Array(Format.toByte(Format.Json)) ++ jsonBytes)

        deserialized shouldBe event
      }
    }
  }

}
