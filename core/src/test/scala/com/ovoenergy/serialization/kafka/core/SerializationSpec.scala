package com.ovoenergy.serialization.kafka.core

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import Serialization._
import Serialization.Implicits._
import com.ovoenergy.serialization.kafka.testkit.UnitSpec
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringSerializer}

class SerializationSpec extends UnitSpec {

  import StandardCharsets._

  val stringSerializer: Serializer[String] = (s: String) => s.getBytes(UTF_8)
  val stringDeserializer: Deserializer[String] = (data: Array[Byte]) => new String(data, UTF_8)

  val intSerializer: Serializer[Int] = {(i: Int) =>
    ByteBuffer.allocate(4).putInt(i).array()
  }

  val intDeserializer: Deserializer[Int] = {(data: Array[Byte]) =>
    ByteBuffer.wrap(data).getInt
  }


  "Serialization" when {

    "deserializing" should {

      "add the magic byte to the serialized data" in {
        formatSerializer(Format.Json, stringSerializer).serialize("test", "Test")(0) should be(Format.toByte(Format.Json))
      }

      "demultiplex the magic byte correctly" in {

        val failingDeserializer = Serialization.deserializer[String]({ _: Array[Byte] => throw new RuntimeException("Wrong or unsupported serialization format byte") })
        val serializer = formatSerializer(Format.Json, stringSerializer)
        val deserializer: Deserializer[String] = formatDemultiplexerDeserializer(failingDeserializer) {
          case Format.Json => formatDroppingDeserializer(stringDeserializer)
          case Format.AvroBinarySchemaId => formatDroppingDeserializer({ data: Array[Byte] => new String(data.map(b => (b + 1).asInstanceOf[Byte]), UTF_8) }) // change the byte value
        }

        val expectedString = "TestString"
        val deserialized = deserializer.deserialize("test-topic", serializer.serialize("test-topic", expectedString))

        deserialized shouldBe expectedString

        val noFormatSerializer = new StringSerializer

        a[RuntimeException] should be thrownBy {
          deserializer.deserialize("test-topic", noFormatSerializer.serialize("test-topic", expectedString))
        }
      }

      "skip the magic byte" in {

        val expectedBytes = "test string".getBytes(UTF_8)
        val deserializer = formatDroppingDeserializer { data: Array[Byte] => data }

        deserializer.deserialize("test-topic", Array(12: Byte) ++ expectedBytes).deep shouldBe expectedBytes.deep
      }

      "demultiplex the topic correctly" in {

        val stringTopic = "string-topic"
        val intTopic = "int-topic"

        // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
        val deserializer = topicDemultiplexerDeserializer(
          TopicMatcher.equalsTo(stringTopic)->stringDeserializer.asInstanceOf[Deserializer[Any]],
          TopicMatcher.equalsTo(intTopic)->intDeserializer.asInstanceOf[Deserializer[Any]]
        )

        val expectedInt = 34

        val deserialized = deserializer.deserialize(intTopic, intSerializer.serialize("Does not matter", expectedInt))

        deserialized shouldBe Some(expectedInt)
      }

    }
  }


}
