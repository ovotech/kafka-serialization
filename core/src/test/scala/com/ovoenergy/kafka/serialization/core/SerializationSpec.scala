package com.ovoenergy.kafka.serialization.core

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.kafka.serialization.core.syntax._
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import scala.util.Random

import SerializationSpec._

object SerializationSpec {

  val IgnoredTopic = "Does not matter"

  val StringTopic = "string-topic"

  val IntTopic = "int-topic"

  val StringSerializer: Serializer[String] = (s: String) => s.getBytes(UTF_8)
  val StringDeserializer: Deserializer[String] = (data: Array[Byte]) => new String(data, UTF_8)

  val IntSerializer: Serializer[Int] = { (i: Int) =>
    ByteBuffer.allocate(4).putInt(i).array()
  }

  val IntDeserializer: Deserializer[Int] = { (data: Array[Byte]) =>
    ByteBuffer.wrap(data).getInt
  }
}


class SerializationSpec extends UnitSpec {

  "Serialization" when {

    "serializing" when {

      "add the magic byte to the serialized data" in {
        formatSerializer(Format.Json, StringSerializer).serialize("test", "Test")(0) should be(Format.toByte(Format.Json))
      }

      "multiplexing the topic" when {
        "the topic matches a branch" should {

          "use the matched serializer" in {

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val serializer = topicMultiplexerSerializer[Any](_ => constSerializer("foo".getBytes(UTF_8))) {
              case StringTopic => StringSerializer.asInstanceOf[Serializer[Any]]
              case IntTopic => IntSerializer.asInstanceOf[Serializer[Any]]
            }

            val serialized = serializer.serialize(IntTopic, 56)

            serialized.deep shouldBe IntSerializer.serialize("Does not Matter", 56).deep
          }
        }

        "the topic does not match any branch" should {

          "use the non matched serializer" in {

            val expectedValue = "foo".getBytes(UTF_8)

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val serializer = topicMultiplexerSerializer[Any](_ => constSerializer(expectedValue)) {
              case StringTopic => StringSerializer.asInstanceOf[Serializer[Any]]
              case IntTopic => IntSerializer.asInstanceOf[Serializer[Any]]
            }

            val serialized = serializer.serialize(IgnoredTopic, 56)

            serialized.deep shouldBe expectedValue.deep
          }
        }
      }

    }

    "deserializing" when {

      "demultiplexing the format" when {

        "the format does not match" should {
          "use the default deserializer" in {

            val expectedFormat = Format.Custom(19)
            val expectedString = "TestString"

            val d: Deserializer[String] = formatDemultiplexerDeserializer(_ => constDeserializer(expectedString)) {
              case Format.Custom(9) => StringDeserializer
              case Format.Custom(8) => constDeserializer("Bad String")
            }

            val deserialized = d.deserialize("test-topic", formatSerializer(expectedFormat, StringSerializer).serialize("", expectedString))

            deserialized shouldBe expectedString
          }
        }

        "dropping format is default" should {
          "drop the magic byte correctly" in {

            val expectedFormat = Format.Custom(9)
            val expectedString = "TestString"

            val d: Deserializer[String] = formatDemultiplexerDeserializer(_ => failingDeserializer[String](new RuntimeException("Wrong or unsupported serialization format byte"))) {
              case `expectedFormat` => StringDeserializer
              case Format.Custom(8) => constDeserializer("Bad String")
            }

            val deserialized = d.deserialize("test-topic", formatSerializer(expectedFormat, StringSerializer).serialize("", expectedString))

            deserialized shouldBe expectedString
          }
        }

        "dropping format is false" should {
          "not drop the magic byte" in {

            val expectedFormat = Format.Custom(9)
            val expectedString = "TestString"

            val d: Deserializer[String] = formatDemultiplexerDeserializer(_ => failingDeserializer[String](new RuntimeException("Wrong or unsupported serialization format byte")), dropFormat = false) {
              case `expectedFormat` => formatCheckingDeserializer(expectedFormat, StringDeserializer)
              case Format.Custom(8) => constDeserializer("Bad String")
            }

            val deserialized = d.deserialize("test-topic", formatSerializer(expectedFormat, StringSerializer).serialize("", expectedString))

            deserialized shouldBe expectedString
          }
        }
      }

      "dropping the magic byte" should {
        "drop the magic byte" in {
          val expectedBytes = "test string".getBytes(UTF_8)
          val deserializer = formatDroppingDeserializer { data: Array[Byte] => data }

          deserializer.deserialize("test-topic", Array(12: Byte) ++ expectedBytes).deep shouldBe expectedBytes.deep
        }
      }

      "checking the magic byte" when {
        "the format matches" should {
          "deserialize successfully" in {

            val expectedValue = "Foo"
            val expectedFormat = Format.Custom(9)

            val deserializer = formatCheckingDeserializer(Format.Custom(9), constDeserializer(expectedValue))

            val deserialized = deserializer.deserialize(IgnoredTopic, Array(expectedFormat.toByte) ++ Array.fill(5)(Random.nextInt().toByte))

            deserialized shouldBe expectedValue
          }
        }

        "the format does not match" should {
          "fail to deserialize" in {

            val unexpectedFormat = Format.Custom(19)

            val deserializer = formatCheckingDeserializer(Format.Custom(9), constDeserializer("Foo"))

            a[UnsupportedFormatException] should be thrownBy deserializer.deserialize(IgnoredTopic, Array(unexpectedFormat.toByte) ++ Array.fill(5)(Random.nextInt().toByte))

          }
        }

        "dropFormat is default" should {
          "drop the format byte" in {

            val expectedFormat = Format.Custom(9)
            val expectedValue: Array[Byte] = Array.fill(5)(Random.nextInt().toByte)

            val deserializer = formatCheckingDeserializer(Format.Custom(9), identityDeserializer)

            val deserialized = deserializer.deserialize(IgnoredTopic, Array(expectedFormat.toByte) ++ expectedValue)

            deserialized.deep shouldBe expectedValue.deep

          }
        }

        "dropFormat is false" should {
          "not dropping the format byte" in {

            val expectedFormat = Format.Custom(9)
            val expectedValue: Array[Byte] = Array(expectedFormat.toByte) ++ Array.fill(5)(Random.nextInt().toByte)

            val deserializer = formatCheckingDeserializer(Format.Custom(9), identityDeserializer, dropFormat = false)

            val deserialized = deserializer.deserialize(IgnoredTopic, expectedValue)

            deserialized.deep shouldBe expectedValue.deep

          }
        }
      }

      "demultiplexing the topic" when {
        "the topic matches a branch" should {
          "use the matched deserializer" in {

            val expectedInt = 34

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val deserializer = topicDemultiplexerDeserializer[Any](topic => failingDeserializer(new IllegalArgumentException(topic))) {
              case StringTopic => StringDeserializer.asInstanceOf[Deserializer[Any]]
              case IntTopic => IntDeserializer.asInstanceOf[Deserializer[Any]]
            }

            val deserialized = deserializer.deserialize(IntTopic, IntSerializer.serialize(IgnoredTopic, expectedInt))

            deserialized shouldBe expectedInt
          }
        }

        "the topic does not match any branch" should {
          "use the non matching deserializer" in {

            val nonMatchingTopic = "test-topic"

            val expectedValue = "test-value"

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val deserializer = topicDemultiplexerDeserializer[Any](_ => constDeserializer(expectedValue)) {
              case StringTopic => StringDeserializer.asInstanceOf[Deserializer[Any]]
              case IntTopic => IntDeserializer.asInstanceOf[Deserializer[Any]]
            }

            val deserialized = deserializer.deserialize(nonMatchingTopic, IntSerializer.serialize(IgnoredTopic, 45))

            deserialized shouldBe expectedValue
          }
        }

      }

    }
  }


}
