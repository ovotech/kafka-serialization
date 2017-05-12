package com.ovoenergy.kafka.serialization.core

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core.syntax._
import com.ovoenergy.kafka.serialization.testkit.UnitSpec
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import scala.util.Random

class SerializationSpec extends UnitSpec {

  import StandardCharsets._

  val stringSerializer: Serializer[String] = (s: String) => s.getBytes(UTF_8)
  val stringDeserializer: Deserializer[String] = (data: Array[Byte]) => new String(data, UTF_8)

  val intSerializer: Serializer[Int] = { (i: Int) =>
    ByteBuffer.allocate(4).putInt(i).array()
  }

  val intDeserializer: Deserializer[Int] = { (data: Array[Byte]) =>
    ByteBuffer.wrap(data).getInt
  }

  val IgnoredTopic = "Does not matter"

  val StringTopic = "string-topic"

  val IntTopic = "int-topic"


  "Serialization" when {

    "serializing" when {

      "add the magic byte to the serialized data" in {
        formatSerializer(Format.Json, stringSerializer).serialize("test", "Test")(0) should be(Format.toByte(Format.Json))
      }

      "multiplexing the topic" when {
        "the topic matches a branch" should {

          "use the matched serializer" in {

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val serializer = topicMultiplexerSerializer[Any](_ => constSerializer("foo".getBytes(UTF_8))) {
              case StringTopic => stringSerializer.asInstanceOf[Serializer[Any]]
              case IntTopic => intSerializer.asInstanceOf[Serializer[Any]]
            }

            val serialized = serializer.serialize(IntTopic, 56)

            serialized.deep shouldBe intSerializer.serialize("Does not Matter", 56).deep
          }
        }

        "the topic does not match any branch" should {

          "use the non matched serializer" in {

            val expectedValue = "foo".getBytes(UTF_8)

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val serializer = topicMultiplexerSerializer[Any](_ => constSerializer(expectedValue)) {
              case StringTopic => stringSerializer.asInstanceOf[Serializer[Any]]
              case IntTopic => intSerializer.asInstanceOf[Serializer[Any]]
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

            val ExpectedFormat = Format.Custom(19)
            val ExpectedString = "TestString"

            val d: Deserializer[String] = formatDemultiplexerDeserializer(_ => constDeserializer(ExpectedString)) {
              case Format.Custom(9) => stringDeserializer
              case Format.Custom(8) => constDeserializer("Bad String")
            }

            val deserialized = d.deserialize("test-topic", formatSerializer(ExpectedFormat, stringSerializer).serialize("", ExpectedString))

            deserialized shouldBe ExpectedString
          }
        }

        "dropping format is default" should {
          "drop the magic byte correctly" in {

            val ExpectedFormat = Format.Custom(9)
            val ExpectedString = "TestString"

            val d: Deserializer[String] = formatDemultiplexerDeserializer(_ => failingDeserializer[String](new RuntimeException("Wrong or unsupported serialization format byte"))) {
              case ExpectedFormat => stringDeserializer
              case Format.Custom(8) => constDeserializer("Bad String")
            }

            val deserialized = d.deserialize("test-topic", formatSerializer(ExpectedFormat, stringSerializer).serialize("", ExpectedString))

            deserialized shouldBe ExpectedString
          }
        }

        "dropping format is false" should {
          "not drop the magic byte" in {

            val ExpectedFormat = Format.Custom(9)
            val ExpectedString = "TestString"

            val d: Deserializer[String] = formatDemultiplexerDeserializer(_ => failingDeserializer[String](new RuntimeException("Wrong or unsupported serialization format byte")), dropFormat = false) {
              case ExpectedFormat => formatCheckingDeserializer(ExpectedFormat, stringDeserializer)
              case Format.Custom(8) => constDeserializer("Bad String")
            }

            val deserialized = d.deserialize("test-topic", formatSerializer(ExpectedFormat, stringSerializer).serialize("", ExpectedString))

            deserialized shouldBe ExpectedString
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

            val StringTopic = "string-topic"
            val IntTopic = "int-topic"

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val deserializer = topicDemultiplexerDeserializer[Any](topic => failingDeserializer(new IllegalArgumentException(topic))) {
              case StringTopic => stringDeserializer.asInstanceOf[Deserializer[Any]]
              case IntTopic => intDeserializer.asInstanceOf[Deserializer[Any]]
            }

            val deserialized = deserializer.deserialize(IntTopic, intSerializer.serialize(IgnoredTopic, expectedInt))

            deserialized shouldBe expectedInt
          }
        }

        "the topic does not match any branch" should {
          "use the non matching deserializer" in {

            val NonMatchingTopic = "test-topic"

            val StringTopic = "string-topic"
            val IntTopic = "int-topic"

            val ExpectedValue = "test-value"

            // This code is nasty, but in production no one is going to have a consumer with two unrelated types.
            val deserializer = topicDemultiplexerDeserializer[Any](_ => constDeserializer(ExpectedValue)) {
              case StringTopic => stringDeserializer.asInstanceOf[Deserializer[Any]]
              case IntTopic => intDeserializer.asInstanceOf[Deserializer[Any]]
            }

            val deserialized = deserializer.deserialize(NonMatchingTopic, intSerializer.serialize(IgnoredTopic, 45))

            deserialized shouldBe ExpectedValue
          }
        }

      }

    }
  }


}
