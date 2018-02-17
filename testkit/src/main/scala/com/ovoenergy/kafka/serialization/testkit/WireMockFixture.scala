/*
 * Copyright 2017 OVO Energy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ovoenergy.kafka.serialization.testkit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockFixture extends BeforeAndAfterAll with BeforeAndAfterEach { self: Suite =>

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

  def resetWireMock(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    wireMockServer.resetScenarios()
  }
}
