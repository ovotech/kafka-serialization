/*
 * Copyright 2017 OVO Energy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ovoenergy.kafka.serialization.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, _}
import com.ovoenergy.kafka.serialization.core._
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, Serializer => KafkaSerializer}

private[jsoniter_scala] trait JsoniterScalaSerialization {

  def jsoniterScalaSerializer[T: JsonValueCodec](config: WriterConfig = WriterConfig()): KafkaSerializer[T] =
    serializer((_, data) => writeToArray[T](data, config))

  def jsoniterScalaDeserializer[T: JsonValueCodec](config: ReaderConfig = ReaderConfig()): KafkaDeserializer[T] =
    deserializer((_, data) => readFromArray(data, config))

}
