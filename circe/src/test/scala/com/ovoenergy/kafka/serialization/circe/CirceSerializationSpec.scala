package com.ovoenergy.kafka.serialization.circe

import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import com.ovoenergy.kafka.serialization.testkit.UnitSpec._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

class CirceSerializationSpec extends UnitSpec with CirceSerialization {

  "CirceSerialization" when {
    "circeJsonSerializer" should {
      "write the Json body" in forAll { event: Event =>
        val serializer = circeJsonSerializer[Event]
        val bytes = serializer.serialize("Does not matter", event)

        parse(new String(bytes, UTF_8)) shouldBe Right(event.asJson)
      }
    }

    "circeJsonDeserializer" should {
      "parse the json" in forAll { event: Event =>
        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializer[Event]
        val deserialized = deserializer.deserialize("does not matter", jsonBytes)

        deserialized shouldBe event
      }
    }

    "circeJsonDeserializerWithFallback" should {
      "parse the json" in forAll { event: Event =>
        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializerWithFallback[Event](_ => Event("", ""))
        val deserialized = deserializer.deserialize("does not matter", jsonBytes)

        deserialized shouldBe event
      }
      "execute fallback function in case of failure" in forAll { event: Event =>
        val jsonBytes = "{}".getBytes(UTF_8)
        val deserializer = circeJsonDeserializerWithFallback[Event](_ => Event("", ""))
        val deserialized = deserializer.deserialize("does not matter", jsonBytes)

        deserialized shouldBe Event("", "")
      }
    }
  }

}
