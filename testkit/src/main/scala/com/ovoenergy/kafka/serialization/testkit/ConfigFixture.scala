package com.ovoenergy.kafka.serialization.testkit

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterEach, Suite}

trait ConfigFixture extends BeforeAndAfterEach { self: Suite =>

  protected def initConfig(): Config = ConfigFactory.load()

  private var mutableConfig: Config = _

  def optionalConfig = Option(mutableConfig)

  def config: Config = optionalConfig.getOrElse(throw new IllegalStateException("Config is not yet initialized"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    mutableConfig = initConfig()
  }
}
