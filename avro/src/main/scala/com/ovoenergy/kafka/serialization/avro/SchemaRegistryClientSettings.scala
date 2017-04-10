package com.ovoenergy.kafka.serialization.avro

case class SchemaRegistryClientSettings(endpoint: String, authentication: Authentication, maxCacheSize: Int)

object SchemaRegistryClientSettings {

  val DefaultCacheSize: Int = 12

  val DefaultAuthentication: Authentication = Authentication.None

  def apply(endpoint: String): SchemaRegistryClientSettings = SchemaRegistryClientSettings(endpoint, DefaultAuthentication, DefaultCacheSize)

  def apply(endpoint: String, username: String, password: String): SchemaRegistryClientSettings = SchemaRegistryClientSettings(endpoint, Authentication.Basic(username, password), DefaultCacheSize)

  def apply(endpoint: String, maxCacheSize: Int): SchemaRegistryClientSettings = SchemaRegistryClientSettings(endpoint, DefaultAuthentication, maxCacheSize)

}

sealed trait Authentication

object Authentication {

  case object None extends Authentication

  case class Basic(username: String, password: String) extends Authentication

}

