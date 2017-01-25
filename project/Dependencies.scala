import sbt._

object Dependencies {

  object Typesafe {
    val config = "com.typesafe" % "config" % "1.3.1"
  }

  object slf4j {

    private val version = "1.7.22"

    val api = "org.slf4j" % "slf4j-api" % version
    val log4jOverSlf4j = "org.slf4j" % "log4j-over-slf4j" % version
    val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % version
    val nop = "org.slf4j" % "slf4j-nop" % version
  }

  object log4j {
    private val version = "2.7"

    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % version
  }

  object logback {

    private val version = "1.1.8"

    val core = "ch.qos.logback" % "logback-core" % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.13.4"

  object scalaMock {

    private val version = "3.4.2"

    val scalaTestSupport = "org.scalamock" %% "scalamock-scalatest-support" % version
  }

  object kafka {

    private val version = "0.10.0.1"

    val avroSerializer = "io.confluent" % "kafka-avro-serializer" % "3.1.1" exclude("org.slf4j", "slf4j-log4j12")
    val client = "org.apache.kafka" % "kafka-clients" % version exclude("org.slf4j", "slf4j-log4j12")
  }

  val dockerClient = "com.spotify" % "docker-client" % "7.0.0"

  object Circe {

    private val version = "0.6.1"

    val core = "io.circe" %% "circe-core" % version
    val generic = "io.circe" %% "circe-generic" % version
    val parser = "io.circe" %% "circe-parser" % version
  }

  object Avro4s {

    private val version = "1.6.3"

    val core = "com.sksamuel.avro4s" %% "avro4s-core" % version
    val macros = "com.sksamuel.avro4s" %% "avro4s-macros" % version
    val json = "com.sksamuel.avro4s" %% "avro4s-json" % version
  }

  object Json4s {

    private val version = "3.5.0"

    val core = "org.json4s" %% "json4s-core" % version
    val native = "org.json4s" %% "json4s-native" % version
  }

  val wiremock = "com.github.tomakehurst" % "wiremock" % "2.4.1"

}
