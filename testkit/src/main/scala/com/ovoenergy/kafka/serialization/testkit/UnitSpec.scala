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
