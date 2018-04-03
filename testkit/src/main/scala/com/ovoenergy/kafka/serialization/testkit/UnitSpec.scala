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

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import org.scalatest.concurrent.{ScalaFutures, ScaledTimeSpans}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

object UnitSpec {

  case class Event(id: String, name: String)

  implicit val arbString: Arbitrary[String] = Arbitrary(for {
    length <- Gen.chooseNum(3, 64)
    chars <- Gen.listOfN(length, Gen.alphaNumChar)
  } yield chars.mkString)

  implicit val arbEvent: Arbitrary[Event] = Arbitrary(for {
    id <- arbitrary[String]
    name <- arbitrary[String]
  } yield Event(id, name))

}

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
