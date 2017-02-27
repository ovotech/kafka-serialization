package com.ovoenergy.serialization.kafka.client.producer

import com.ovoenergy.serialization.kafka.client.util.DurationUtils
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

case class ProducerName(value: String)

private[producer] case class ProducerConfig(initialDelay: FiniteDuration, interval: FiniteDuration, producerName: ProducerName)

object ProducerConfig extends DurationUtils {

  def apply(config: Config, producerName: ProducerName): ProducerConfig = {
    val initialDelay = getFiniteDuration(config.getString("kafka.producer.initialDelay"))
    val interval = getFiniteDuration(config.getString("kafka.producer.interval"))
    ProducerConfig(initialDelay, interval, producerName)
  }

}
