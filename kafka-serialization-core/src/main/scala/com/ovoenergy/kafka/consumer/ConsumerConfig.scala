package com.ovoenergy.kafka.consumer

import akka.util.Timeout
import com.ovoenergy.kafka.util.DurationUtils
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

private[consumer] case class ConsumerConfig(initialDelay: FiniteDuration,
                                            interval: FiniteDuration,
                                            topic: String,
                                            clientId: String,
                                            gropuId: String,
                                            pollingTimeout: Long,
                                            consumerName: String,
                                            askTimeout: Timeout)

private[kafka] object ConsumerConfig extends DurationUtils {

  def apply(config: Config, topic: String, clientId: String): ConsumerConfig = {
    val initialDelay = getFiniteDuration(config.getString("kafka.consumer.initialDelay"))
    val interval = getFiniteDuration(config.getString("kafka.consumer.interval"))
    val groupId = config.getString("kafka.consumer.properties.group.id")
    val pollingTimeout = config.getLong("kafka.consumer.pollingTimeoutMs")
    val consumerName = s"$groupId.$clientId.$topic"
    val askTimeout = new Timeout(getFiniteDuration("kafka.consumer.askTimeout"))
    ConsumerConfig(initialDelay, interval, topic, clientId, groupId, pollingTimeout, consumerName, askTimeout)
  }

}
