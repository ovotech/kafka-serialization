import Dependencies._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._
import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val `kafka-serialization-magic` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(
    organization := "com.ovoenergy",
    organizationHomepage := Some(url("https://www.ovoenergy.com/")),
    name := "kafka-serialization-magic",
    homepage := Some(url("https://github.com/ovotech/kafka-serialization")),
    startYear := Some(2016),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ovotech/kafka-serialization"),
        "git@github.com:ovotech/kafka-serialization.git"
      )
    ),
    git.remoteRepo := "origin",
    git.runner := ConsoleGitRunner,
    git.baseVersion := "0.1.0",
    git.useGitDescribe := true,
    scalaVersion := "2.12.1",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.typesafeRepo("releases"),
      "confluent-release" at "http://packages.confluent.io/maven/"
    ),
    libraryDependencies ++= Seq(
      kafka.client,
      kafka.avroSerializer,
      Circe.core,
      Circe.parser,
      Avro4s.core,
      Json4s.core,
      Json4s.native,
      // -- Testing --
      Typesafe.config % Test,
      scalaTest % Test,
      scalaCheck % Test,
      scalaMock.scalaTestSupport % Test,
      logback.classic % Test,
      wiremock % Test,
      Circe.generic % Test
    ),
    headers := Map(
      "java" -> Apache2_0("2017", "OVO Energy"),
      "scala" -> Apache2_0("2017", "OVO Energy"),
      "conf" -> Apache2_0("2017", "OVO Energy", "#")
    ),
    tutSettings,
    tutTargetDirectory := baseDirectory.value,
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("kafka", "serialization")
  )
