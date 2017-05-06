package com.ovoenergy.kafka.serialization.testkit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockFixture extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>

  private lazy val wireMockServer: WireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort())

  val wireMockHost: String = "localhost"
  def wireMockPort: Int = wireMockServer.port()
  def wireMockEndpoint: String = s"http://$wireMockHost:$wireMockPort"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(wireMockPort)
  }

  override protected def afterAll(): Unit = {
    wireMockServer.shutdown()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    resetWireMock()
    super.afterEach()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  def resetWireMock(): Unit ={
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    wireMockServer.resetScenarios()
  }
}