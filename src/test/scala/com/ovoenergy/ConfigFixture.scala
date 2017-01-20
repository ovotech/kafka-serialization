package com.ovoenergy

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterEach, Suite}

trait ConfigFixture extends BeforeAndAfterEach { self: Suite =>

  protected def initConfig(): Config = ConfigFactory.load()

  private var _config: Config = _
  def optionalConfig = Option(_config)
  def config: Config = optionalConfig.getOrElse(throw new IllegalStateException("Config not yet initialized"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    _config = initConfig()
  }
}
