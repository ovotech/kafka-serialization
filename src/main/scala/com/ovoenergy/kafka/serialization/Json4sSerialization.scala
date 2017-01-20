package com.ovoenergy.kafka.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.ovoenergy.kafka.serialization.Serialization._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object Json4sSerialization {

  def serializeWithJson4sJson[T <: AnyRef](implicit jsonFormats: Formats): KafkaSerializer[T] = serializerWithMagicByte(Format.Json, serializer { (_, data) =>
    val bout = new ByteArrayOutputStream()
    write[T, OutputStreamWriter](data, new OutputStreamWriter(bout, StandardCharsets.UTF_8))
    bout.flush()
    bout.toByteArray
  })

  def deserializeWithJson4s[T: TypeTag](implicit jsonFormats: Formats): KafkaDeserializer[T] = deserializerWithFirstByteDropping(deserializer { (_, data) =>
    val tt = implicitly[TypeTag[T]]
    implicit val cl = ClassTag[T](tt.mirror.runtimeClass(tt.tpe))
    read[T](new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))
  })


}
