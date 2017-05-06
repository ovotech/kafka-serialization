package com.ovoenergy.kafka.serialization.circe

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core.Format
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import io.circe.generic.auto._
import io.circe.jawn.JawnParser
import io.circe.syntax._

class CirceSerializationSpec extends UnitSpec with CirceSerialization {

  import StandardCharsets._

  import com.ovoenergy.kafka.serialization.testkit.UnitSpec._

  "CirceSerialization" when {
    "serializing" should {
      "put the Json format byte before the body" in forAll { event: Event =>

        val serializer = circeJsonSerializer[Event]
        val bytes = serializer.serialize("Does not matter", event)

        bytes(0) shouldBe Format.toByte(Format.Json)
      }

      "put the Json body after the format byte" in forAll { event: Event =>

        val serializer = circeJsonSerializer[Event]
        val bytes = serializer.serialize("Does not matter", event)

        new JawnParser().parseByteBuffer(ByteBuffer.wrap(bytes.drop(1))) shouldBe Right(event.asJson)
      }
    }

    "deserializing" should {
      "raise an error if the format byte does not match" in forAll { event: Event =>

        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializer[Event]

        an[Exception] should be thrownBy deserializer.deserialize("does not matter", Array(90: Byte) ++ jsonBytes)
      }

      "parse the json after the format byte" in forAll { event: Event =>

        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializer[Event]

        val deserialized = deserializer.deserialize("does not matter", Array(Format.toByte(Format.Json)) ++ jsonBytes)

        deserialized shouldBe event
      }
    }
  }

}
