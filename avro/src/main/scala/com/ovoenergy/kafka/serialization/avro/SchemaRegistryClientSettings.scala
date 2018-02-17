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

package com.ovoenergy.kafka.serialization.avro

case class SchemaRegistryClientSettings(endpoint: String,
                                        authentication: Authentication,
                                        maxCacheSize: Int,
                                        cacheParallelism: Int)

object SchemaRegistryClientSettings {

  val DefaultCacheSize: Int = 12

  val DefaultCacheParallelism: Int = 4

  val DefaultAuthentication: Authentication = Authentication.None

  def apply(endpoint: String): SchemaRegistryClientSettings =
    SchemaRegistryClientSettings(endpoint, DefaultAuthentication, DefaultCacheSize, DefaultCacheParallelism)

  def apply(endpoint: String, username: String, password: String): SchemaRegistryClientSettings =
    SchemaRegistryClientSettings(
      endpoint,
      Authentication.Basic(username, password),
      DefaultCacheSize,
      DefaultCacheParallelism
    )

  def apply(endpoint: String, maxCacheSize: Int): SchemaRegistryClientSettings =
    SchemaRegistryClientSettings(endpoint, DefaultAuthentication, maxCacheSize, DefaultCacheParallelism)

}

sealed trait Authentication

object Authentication {

  case object None extends Authentication

  case class Basic(username: String, password: String) extends Authentication

}
