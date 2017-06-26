package com.ovoenergy.kafka.serialization

/**
  * Aggregates basic blocks to build kafka serializers and deserializers from the extended traits.
  */
package object core extends Serialization with Deserialization
