package com.ovoenergy

import org.scalatest.concurrent.{ScalaFutures, ScaledTimeSpans}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

abstract class UnitSpec
    extends WordSpec
    with Matchers
    with PropertyChecks
    with ScalaFutures
    with ScaledTimeSpans
    with ConfigFixture {

  // AbstractPatienceConfiguration define a class that call span
  override def spanScaleFactor: Double = optionalConfig.getOrElse(initConfig()).getDouble("akka.test.timefactor")
}
