package com.ovoenergy.serialization.kafka.client.consumer

import akka.util.Timeout
import com.ovoenergy.serialization.kafka.client.util.DurationUtils
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

case class Topic(value: String)

case class ClientId(value: String)

case class GroupId(value: String)

case class ConsumerName(value: String)

private[consumer] case class ConsumerConfig(initialDelay: FiniteDuration,
                                            interval: FiniteDuration,
                                            topics: Seq[Topic],
                                            clientId: ClientId,
                                            groupId: GroupId,
                                            pollingTimeout: Long,
                                            consumerName: ConsumerName,
                                            askTimeout: Timeout)

object ConsumerConfig extends DurationUtils {

  def apply(config: Config, consumerName: ConsumerName, clientId: ClientId, topics: Topic*): ConsumerConfig = {
    val initialDelay = getFiniteDuration(config.getString("kafka.consumer.initialDelay"))
    val interval = getFiniteDuration(config.getString("kafka.consumer.interval"))
    val groupId = GroupId(config.getString("kafka.consumer.properties.group.id"))
    val pollingTimeout = config.getLong("kafka.consumer.pollingTimeoutMs")
    val askTimeout = new Timeout(getFiniteDuration(config.getString("kafka.consumer.askTimeout")))
    ConsumerConfig(initialDelay, interval, topics, clientId, groupId, pollingTimeout, consumerName, askTimeout)
  }

}
