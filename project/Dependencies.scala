import sbt.Keys._
import sbt._

object Dependencies {

  object Cats {

    private val version = "1.0.1"

    val core = "org.typelevel" %% "cats-core" % version
  }

  object Akka {

    private val version = "2.4.17"

    val actor = "com.typesafe.akka" %% s"akka-actor" % version
    val stream = "com.typesafe.akka" %% s"akka-stream" % version
    val testKit = "com.typesafe.akka" %% s"akka-testkit" % version

  }

  object Typesafe {

    private val version = "1.3.1"

    val config = "com.typesafe" % "config" % version
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

  object scalaTest {

    private val version = "3.0.1"

    val scalaTest = "org.scalatest" %% "scalatest" % version

  }

  object scalaCheck {

    private val version = "1.13.4"

    val scalaCheck = "org.scalacheck" %% "scalacheck" % version

  }

  object scalaMock {

    private val version = "3.4.2"

    val scalaTestSupport = "org.scalamock" %% "scalamock-scalatest-support" % version
  }

  val scalaArm = "com.jsuereth" %% "scala-arm" % "2.0"

  object kafka {

    private val version = "0.11.0.1"

    val avroSerializer = "io.confluent" % "kafka-avro-serializer" % "4.0.0" exclude ("org.slf4j", "slf4j-log4j12")
    val client = "org.apache.kafka" % "kafka-clients" % version exclude ("org.slf4j", "slf4j-log4j12")
  }

  object JsoniterScala {

    private val version = "0.22.2"

    val macros = "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % version
  }

  object Circe {

    private val version = "0.8.0"

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

    private val version = "3.5.1"

    val core = "org.json4s" %% "json4s-core" % version
    val native = "org.json4s" %% "json4s-native" % version
  }

  object Wiremock {

    private val version = "2.6.0"

    val wiremock = "com.github.tomakehurst" % "wiremock" % version

  }

  object Spray {

    private val version = "1.3.3"

    val json = "io.spray" %% "spray-json" % version

  }

  object Jersey {

    private val version = "2.25.1"

    val client = "org.glassfish.jersey.core" % "jersey-client" % version
    val jsonProcessing = "org.glassfish.jersey.media" % "jersey-media-json-processing" % version
    val apacheConnector = "org.glassfish.jersey.connectors" % "jersey-apache-connector" % version
    val nettyConnector = "org.glassfish.jersey.connectors" % "jersey-netty-connector" % version

  }

  val l = libraryDependencies

  val core = l ++= Seq(
    kafka.client
  )

  val cats = l ++= Seq(
    kafka.client,
    Cats.core
  )

  val json4s = l ++= Seq(Json4s.core, Json4s.native)

  val avro = l ++= Seq(
    kafka.avroSerializer,
    Jersey.client,
    Jersey.nettyConnector,
    Jersey.jsonProcessing,
    scalaArm % Test
  )

  val avro4s = l ++= Seq(Avro4s.core, kafka.avroSerializer)

  val jsoniterScala = l ++= Seq(JsoniterScala.macros)

  val circe = l ++= Seq(Circe.core, Circe.parser, Circe.generic % Test)

  val spray = l ++= Seq(Spray.json)

  val testkit = l ++= Seq(
    Akka.testKit,
    scalaTest.scalaTest,
    scalaCheck.scalaCheck,
    Typesafe.config,
    scalaMock.scalaTestSupport,
    logback.classic,
    Wiremock.wiremock
  )

  val doc = l ++= Seq(Circe.generic % "tut")
}
