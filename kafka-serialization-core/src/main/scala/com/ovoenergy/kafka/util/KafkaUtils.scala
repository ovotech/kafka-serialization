package com.ovoenergy.kafka.util

trait KafkaUtils {

  /**
    * Method for retrieving kafka failed topic for given group and customer ids.
    *
    * Failed message will be enqueued to failure topic for later processing.
    *
    * @param groupId  consumer group id
    * @param clientId consumer client id
    * @return failed topic name
    */
  def failedTopic(groupId: String, clientId: String): String = topicWithGroupId(groupId, clientId, "failed")

  /**
    * Method for retrieving kafka requeued topic for given group and customer ids.
    *
    * Messages enqueued to failure topic can be manually transferred to requeued topic for later processing.
    *
    * @param groupId  consumer group id
    * @param clientId consumer client id
    * @return requeued topic name
    */
  def requeuedTopic(groupId: String, clientId: String): String = topicWithGroupId(groupId, clientId, "requeued")

  private def topicWithGroupId(groupId: String, clientId: String, suffix: String): String = s"$groupId.$clientId.$suffix"

}

object KafkaUtils extends KafkaUtils
