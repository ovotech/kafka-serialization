package com.ovoenergy.kafka.producer

import com.ovoenergy.kafka.util.DurationUtils
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

private[producer] case class ProducerConfig(initialDelay: FiniteDuration, interval: FiniteDuration, producerName: String)

private[kafka] object ProducerConfig extends DurationUtils {

  def apply(config: Config): ProducerConfig = {
    val initialDelay = getFiniteDuration(config.getString("kafka.producer.initialDelay"))
    val interval = getFiniteDuration(config.getString("kafka.producer.interval"))
    val producerName = config.getString("kafka.producer.name")
    ProducerConfig(initialDelay, interval, producerName)
  }

}
