package com.ovoenergy.serialization.kafka.client.producer

import com.ovoenergy.serialization.kafka.client.consumer.Topic

/**
  * Created by Piotr Fras on 23/02/17.
  */
case class Event[K, V](topic: Topic, key: K, value: V)
