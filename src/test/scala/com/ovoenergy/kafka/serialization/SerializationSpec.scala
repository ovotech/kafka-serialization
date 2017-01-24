package com.ovoenergy.kafka.serialization

import com.ovoenergy.UnitSpec

import Serialization._
import Serialization.Implicits._

class SerializationSpec extends UnitSpec {

  "Serialization" should {
    "add the magic byte to the serialized data" in {
      serializerWithMagicByte(Format.Json, (s: String) => s.getBytes).serialize("test", "Test")(0) should be(Format.toByte(Format.Json))
    }
  }


}
