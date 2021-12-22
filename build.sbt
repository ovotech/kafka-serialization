import sbtrelease.ExtraReleaseCommands
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.tagsonly.TagsOnly._

lazy val catsVersion = "2.1.0"
lazy val circeVersion = "0.11.1"
lazy val logbackVersion = "1.2.7"
lazy val avro4sVersion = "1.9.0"
lazy val avro4s2Version = "2.0.4"
lazy val json4sVersion = "3.6.7"
lazy val slf4jVersion = "1.7.30"
lazy val sprayJsonVersion = "1.3.5"
lazy val kafkaClientVersion = "2.7.2"
lazy val jsoninterScalaVersion = "1.2.0"
lazy val confluentPlatformVersion = "5.3.7"
lazy val scalaTestVersion = "3.0.8"
lazy val scalaCheckVersion = "1.14.3"
lazy val scalaMockVersion = "3.6.0"
lazy val wiremockVersion = "2.32.0"
lazy val scalaArmVersion = "2.0"

lazy val publicArtifactory = "Artifactory Realm" at "https://kaluza.jfrog.io/artifactory/maven"

lazy val publishSettings = Seq(
  publishTo := Some(publicArtifactory),
  credentials += {
    for {
      usr <- sys.env.get("ARTIFACTORY_USER")
      password <- sys.env.get("ARTIFACTORY_PASS")
    } yield Credentials("Artifactory Realm", "kaluza.jfrog.io", usr, password)
  }.getOrElse(Credentials(Path.userHome / ".ivy2" / ".credentials")),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    releaseStepCommand(ExtraReleaseCommands.initialVcsChecksCommand),
    setVersionFromTags(releaseTagPrefix.value),
    runClean,
    tagRelease,
    publishArtifacts,
    pushTagsOnly
  )
)

lazy val `kafka-serialization` = project
  .in(file("."))
  .aggregate(avro, avro4s, avro4s2, cats, circe, core, json4s, `jsoniter-scala`, spray, testkit, doc)
  .settings(
    inThisBuild(
      List(
        organization := "com.ovoenergy",
        organizationName := "OVO Energy",
        organizationHomepage := Some(url("https://www.ovoenergy.com/")),
        homepage := Some(url("https://github.com/ovotech/kafka-serialization")),
        startYear := Some(2017),
        licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
        scmInfo := Some(
          ScmInfo(
            url("https://github.com/ovotech/kafka-serialization"),
            "git@github.com:ovotech/kafka-serialization.git"
          )
        ),
        // TODO Find a way to extract those from github (sbt plugin)
        developers := List(
          Developer(
            "filippo.deluca",
            "Filippo De Luca",
            "filippo.deluca@ovoenergy.com",
            url("https://github.com/filosganga")
          )
        ),
        scalaVersion := "2.12.8",
        resolvers ++= Seq(
          Resolver.mavenLocal,
          Resolver.typesafeRepo("releases"),
          "confluent-release" at "https://packages.confluent.io/maven/",
          "redhat-ga" at "https://maven.repository.redhat.com/ga/"
        )
      )
    )
  )
  .settings(name := "kafka-serialization", publishArtifact := false, publish := {})
  .settings(publishSettings)

lazy val doc = project
  .in(file("doc"))
  .enablePlugins(MdocPlugin)
  .dependsOn(avro, avro4s, cats, circe, core, json4s, `jsoniter-scala`, spray)
  .settings(
    name := "kafka-serialization-doc",
    publishArtifact := false,
    publish := {},
    mdocIn := baseDirectory.value / "src",
    mdocOut := (baseDirectory.value).getParentFile,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % circeVersion,
      "org.apache.kafka" % "kafka-clients" % kafkaClientVersion exclude ("org.slf4j", "slf4j-log4j12"),
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoninterScalaVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoninterScalaVersion % Provided
    )
  )
  .settings(publishSettings)

lazy val testkit = project
  .in(file("testkit"))
  .settings(
    name := "kafka-serialization-testkit",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion,
      "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion,
      "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion,
      "com.jsuereth" %% "scala-arm" % scalaArmVersion,
      "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
      "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
      "ch.qos.logback" % "logback-core" % logbackVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )
  .settings(publishSettings)

lazy val json4s = project
  .in(file("json4s"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-json4s",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.json4s" %% "json4s-core" % json4sVersion,
      "org.json4s" %% "json4s-native" % json4sVersion
    )
  )
  .settings(publishSettings)

lazy val avro = project
  .in(file("avro"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-avro",
    libraryDependencies ++= Seq(
      "io.confluent" % "kafka-avro-serializer" % confluentPlatformVersion exclude ("org.slf4j", "slf4j-log4j12")
    )
  )
  .settings(publishSettings)

lazy val avro4s = project
  .in(file("avro4s"))
  .dependsOn(core, avro, testkit % Test)
  .settings(
    name := "kafka-serialization-avro4s",
    libraryDependencies ++= Seq(
      "com.sksamuel.avro4s" %% "avro4s-macros" % avro4sVersion,
      "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion,
      "com.sksamuel.avro4s" %% "avro4s-json" % avro4sVersion
    )
  )
  .settings(publishSettings)

lazy val avro4s2 = project
  .in(file("avro4s2"))
  .dependsOn(core, avro, testkit % Test)
  .settings(
    name := "kafka-serialization-avro4s2",
    libraryDependencies ++= Seq(
      "com.sksamuel.avro4s" %% "avro4s-macros" % avro4s2Version,
      "com.sksamuel.avro4s" %% "avro4s-core" % avro4s2Version,
      "com.sksamuel.avro4s" %% "avro4s-json" % avro4s2Version
    )
  )
  .settings(publishSettings)

lazy val `jsoniter-scala` = project
  .in(file("jsoniter-scala"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-jsoniter-scala",
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoninterScalaVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoninterScalaVersion % Provided
    )
  )
  .settings(publishSettings)

lazy val circe = project
  .in(file("circe"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion % Test
    )
  )
  .settings(publishSettings)

lazy val spray = project
  .in(file("spray"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-spray",
    libraryDependencies ++= Seq("io.spray" %% "spray-json" % sprayJsonVersion)
  )
  .settings(publishSettings)

lazy val core = project
  .in(file("core"))
  .dependsOn(testkit % Test)
  .settings(
    name := "kafka-serialization-core",
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % kafkaClientVersion exclude ("org.slf4j", "slf4j-log4j12"),
      "org.slf4j" % "slf4j-api" % slf4jVersion
    )
  )
  .settings(publishSettings)

lazy val cats = project
  .in(file("cats"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-cats",
    libraryDependencies ++= Seq("org.typelevel" %% "cats-core" % catsVersion)
  )
  .settings(publishSettings)
