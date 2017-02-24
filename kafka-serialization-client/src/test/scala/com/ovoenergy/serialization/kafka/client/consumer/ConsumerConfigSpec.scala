package com.ovoenergy.serialization.kafka.client.consumer

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.ovoenergy.serialization.kafka.client.util.DurationUtils
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}

class ConsumerConfigSpec extends WordSpec with ScalaFutures with PatienceConfiguration {

  trait ConsumerConfigSpecContext extends DurationUtils {
    val pollingTimeout = 1000L
    val initialDelay = "500 milliseconds"
    val interval = initialDelay
    val askTimeout = "5 seconds"
    val topic = Topic("topic")
    val clientId = ClientId("clientId")
    val groupId = "groupId"
    val name = ConsumerName(s"$groupId.$clientId.$topic")
    val config = ConsumerConfig(ConfigFactory.parseString(
      s"""
         |kafka {
         |  consumer {
         |    initialDelay = "$initialDelay"
         |    interval = "$interval"
         |    pollingTimeoutMs = $pollingTimeout
         |    askTimeout = "$askTimeout"
         |    properties {
         |      group.id = "$groupId"
         |    }
         |  }
         |}
       """.stripMargin), name, clientId, topic)
  }

  "ConsumerConfigSpec" should {

    "parse consumer config" in new ConsumerConfigSpecContext {
      config === ConsumerConfig(getFiniteDuration(initialDelay), getFiniteDuration(interval), Seq(topic), clientId, GroupId(groupId), pollingTimeout, name, Timeout(5, TimeUnit.SECONDS))
    }

  }

}
