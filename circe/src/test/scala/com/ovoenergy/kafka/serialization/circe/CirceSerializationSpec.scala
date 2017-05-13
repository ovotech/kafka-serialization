package com.ovoenergy.kafka.serialization.circe

import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import com.ovoenergy.kafka.serialization.testkit.UnitSpec._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

class CirceSerializationSpec extends UnitSpec with CirceSerialization {


  "CirceSerialization" when {
    "serializing" should {
      "write the Json body" in forAll { event: Event =>

        val serializer = circeJsonSerializer[Event]
        val bytes = serializer.serialize("Does not matter", event)

        parse(new String(bytes, UTF_8)) shouldBe Right(event.asJson)
      }
    }

    "deserializing" should {
      "parse the json" in forAll { event: Event =>

        val jsonBytes = event.asJson.noSpaces.getBytes(UTF_8)
        val deserializer = circeJsonDeserializer[Event]

        val deserialized = deserializer.deserialize("does not matter", jsonBytes)

        deserialized shouldBe event
      }
    }
  }

}
