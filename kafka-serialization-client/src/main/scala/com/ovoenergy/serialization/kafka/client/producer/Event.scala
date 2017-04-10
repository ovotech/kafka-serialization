package com.ovoenergy.serialization.kafka.client.producer

import com.ovoenergy.serialization.kafka.client.Topic

case class Event[K, V](topic: Topic, key: K, value: V)
