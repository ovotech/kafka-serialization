package com.ovoenergy.kafka.util

import java.util.Properties

import com.typesafe.config.Config

import scala.collection.JavaConversions._

trait ConfigUtils {

  /**
    * Converts config to properties.
    *
    * @return properties from config
    */
  def propertiesFrom(config: Config): Properties = {
    val properties = new Properties()
    config.entrySet().map { entry => (entry.getKey, entry.getValue.unwrapped().toString) }.foreach { case (key, value) => properties.put(key, value) }
    properties
  }

}

object ConfigUtils extends ConfigUtils
