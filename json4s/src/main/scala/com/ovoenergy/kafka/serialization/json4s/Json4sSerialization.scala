package com.ovoenergy.kafka.serialization.json4s

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.core._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait Json4sSerialization {

  def json4sSerializer[T <: AnyRef](implicit jsonFormats: Formats): KafkaSerializer[T] = serializer { (_, data) =>
    val bout = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(bout, StandardCharsets.UTF_8)

    // TODO Use scala-arm
    try {
      write(data, writer)
      writer.flush()
    } finally {
      writer.close()
    }
    bout.toByteArray
  }

  def json4sDeserializer[T: TypeTag](implicit jsonFormats: Formats): KafkaDeserializer[T] = deserializer { (_, data) =>
    val tt = implicitly[TypeTag[T]]
    implicit val cl = ClassTag[T](tt.mirror.runtimeClass(tt.tpe))
    read[T](new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))
  }

}
