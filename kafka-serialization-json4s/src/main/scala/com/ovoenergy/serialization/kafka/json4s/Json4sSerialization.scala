package com.ovoenergy.serialization.kafka.json4s

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.ovoenergy.serialization.kafka.core.Serialization._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object Json4sSerialization {

  def json4sSerializer[T <: AnyRef](implicit jsonFormats: Formats): KafkaSerializer[T] = formatSerializer(Format.Json, serializer { (_, data) =>
    val bout = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(bout, StandardCharsets.UTF_8)
    try {
      write(data, writer)
      writer.flush()
    } finally {
      writer.close()
    }
    bout.toByteArray
  })

  def json4sDeserializer[T: TypeTag](implicit jsonFormats: Formats): KafkaDeserializer[T] = formatCheckingDeserializer(Format.Json, deserializer { (_, data) =>
    val tt = implicitly[TypeTag[T]]
    implicit val cl = ClassTag[T](tt.mirror.runtimeClass(tt.tpe))
    read[T](new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))
  })


}
