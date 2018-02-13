package com.ovoenergy.kafka.serialization.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, _}
import com.ovoenergy.kafka.serialization.core._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

private[jsoniter_scala] trait JsoniterScalaSerialization {

  def jsoniterScalaSerializer[T](
    config: WriterConfig = WriterConfig()
  )(implicit codec: JsonCodec[T]): KafkaSerializer[T] = serializer((_, data) => write[T](data, config))

  def jsoniterScalaDeserializer[T](
    config: ReaderConfig = ReaderConfig()
  )(implicit codec: JsonCodec[T]): KafkaDeserializer[T] = deserializer((_, data) => read(data, config))

}
