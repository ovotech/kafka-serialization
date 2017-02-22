package com.ovoenergy.kafka.serialization

import java.util
import java.util.UUID

import com.ovoenergy.kafka.model.Event._
import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster

class PartitionKeyPartitioner extends Partitioner {

  /**
    * Method for computing partition for the given record.
    * Partition key can be specified to indicate the destination partition of the message.
    */
  override def partition(topic: String, key: scala.Any, keyBytes: Array[Byte], value: scala.Any, valueBytes: Array[Byte], cluster: Cluster): Int = {
    partitionByKey(key.asInstanceOf[Key], cluster.partitionCountForTopic(topic))
  }

  override def configure(configs: util.Map[String, _]): Unit = {}

  override def close(): Unit = {}

  private def partitionByKey(key: Key, noOfPartitions: Int): Int = {
    // Not doing .hashCode() directly otherwise all of None will have the same partition
    key.partitionKey.getOrElse(UUID.randomUUID()).hashCode() % noOfPartitions
  }

}
