package com.ovoenergy.kafka.serialization.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import com.ovoenergy.kafka.serialization.testkit.UnitSpec._

class JsoniterScalaSerializationSpec extends UnitSpec with JsoniterScalaSerialization {

  implicit val eventCodec: JsonCodec[Event] = JsonCodecMaker.make[Event](CodecMakerConfig())

  "JsoniterScalaSerialization" when {
    "serializing" should {
      "write compact json" in forAll { event: Event =>
        val serializer = jsoniterScalaSerializer[Event]()

        val jsonBytes = serializer.serialize("does not matter", event)

        jsonBytes.deep shouldBe JsonWriter.write(eventCodec, event).deep
      }
      "write prettified json" in forAll { event: Event =>
        val serializer = jsoniterScalaSerializer[Event](WriterConfig(indentionStep = 2))

        val jsonBytes = serializer.serialize("does not matter", event)

        jsonBytes.deep shouldBe JsonWriter.write(eventCodec, event, WriterConfig(indentionStep = 2)).deep
      }
    }

    "deserializing" should {
      "parse the json" in forAll { event: Event =>
        val jsonBytes = JsonWriter.write(eventCodec, event)
        val deserializer = jsoniterScalaDeserializer[Event]()

        val deserialized = deserializer.deserialize("does not matter", jsonBytes)

        deserialized shouldBe event
      }
      "throw parse exception with a hex dump in case of invalid input" in {
        val deserializer = jsoniterScalaDeserializer[Event]()

        assert(intercept[JsonParseException] {
          deserializer.deserialize("does not matter", """{"name":"vjTjvnkwbdGczk7ylwtsLzfkawxsydRul9Infmapftuhn"}""".getBytes)
        }.getMessage.contains(
          """missing required field(s) "id", offset: 0x00000037, buf:
            |           +-------------------------------------------------+
            |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
            |+----------+-------------------------------------------------+------------------+
            || 00000010 | 77 62 64 47 63 7a 6b 37 79 6c 77 74 73 4c 7a 66 | wbdGczk7ylwtsLzf |
            || 00000020 | 6b 61 77 78 73 79 64 52 75 6c 39 49 6e 66 6d 61 | kawxsydRul9Infma |
            || 00000030 | 70 66 74 75 68 6e 22 7d                         | pftuhn"}         |
            |+----------+-------------------------------------------------+------------------+""".stripMargin
        ))
      }
      "throw parse exception without a hex dump in case of invalid input" in {
        val deserializer = jsoniterScalaDeserializer[Event](ReaderConfig(appendHexDumpToParseException = false))

        assert(intercept[JsonParseException] {
          deserializer.deserialize("does not matter", """{"name":"vjTjvnkwbdGczk7ylwtsLzfkawxsydRul9Infmapftuhn"}""".getBytes)
        }.getMessage.contains("""missing required field(s) "id", offset: 0x00000037"""))
      }
    }
  }

}
