package com.ovoenergy.serialization.kafka.client

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.mutable.After

import scala.concurrent.Await
import scala.concurrent.duration.Duration

abstract class ActorSpecContext extends TestKit(ActorSystem()) with After with ImplicitSender {
  def after = Await.result(system.terminate(), Duration.Inf)
}
