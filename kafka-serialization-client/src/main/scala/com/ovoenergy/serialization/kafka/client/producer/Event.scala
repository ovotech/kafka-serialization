package com.ovoenergy.serialization.kafka.client.producer

/**
  * Created by Piotr Fras on 23/02/17.
  */
case class Event[K, V](topic: String, key: K, value: V)
