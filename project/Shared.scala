import de.heikoseeberger.sbtheader.HeaderKey.headers
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt.Keys._
import sbt.{Resolver, ScmInfo, url, _}

object Shared {

  lazy val settings = Seq(
    fork in Test := true,
    organization := "com.ovoenergy",
    organizationHomepage := Some(url("https://www.ovoenergy.com/")),
    homepage := Some(url("https://github.com/ovotech/kafka-serialization")),
    startYear := Some(2017),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scmInfo := Some(ScmInfo(url("https://github.com/ovotech/kafka-serialization"), "git@github.com:ovotech/kafka-serialization.git")),
    scalaVersion := "2.12.2",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
    resolvers ++= Seq(Resolver.mavenLocal, Resolver.typesafeRepo("releases"), "confluent-release" at "http://packages.confluent.io/maven/"),
    headers := Map("java" -> Apache2_0("2017", "OVO Energy"), "scala" -> Apache2_0("2017", "OVO Energy"), "conf" -> Apache2_0("2017", "OVO Energy", "#"))
  )

}