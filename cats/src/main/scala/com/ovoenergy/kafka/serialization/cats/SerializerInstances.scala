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

package com.ovoenergy.kafka.serialization.cats

import cats.Contravariant
import com.ovoenergy.kafka.serialization.core._
import org.apache.kafka.common.serialization.Serializer

trait SerializerInstances {

  implicit lazy val catsInstancesForKafkaSerializer: Contravariant[Serializer] = new Contravariant[Serializer] {
    override def contramap[A, B](fa: Serializer[A])(f: B => A): Serializer[B] = serializer[B] { (topic: String, b: B) =>
      fa.serialize(topic, f(b))
    }
  }

}
