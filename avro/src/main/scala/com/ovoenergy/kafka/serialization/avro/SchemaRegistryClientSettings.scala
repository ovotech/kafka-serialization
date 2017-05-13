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
