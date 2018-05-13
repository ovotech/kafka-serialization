import sbt.Keys._
import sbt._

object Shared {

  lazy val settings = Seq(
    organization := "com.ovoenergy",
    organizationName := "OVO Energy",
    organizationHomepage := Some(url("https://www.ovoenergy.com/")),
    homepage := Some(url("https://github.com/ovotech/kafka-serialization")),
    startYear := Some(2017),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scmInfo := Some(
      ScmInfo(url("https://github.com/ovotech/kafka-serialization"), "git@github.com:ovotech/kafka-serialization.git")
    ),
    scalaVersion := "2.12.6",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.typesafeRepo("releases"),
      Resolver.bintrayRepo("tpolecat", "maven"),
      "confluent-release" at "http://packages.confluent.io/maven/"
    )
  )

}
