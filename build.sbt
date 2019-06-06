lazy val catsVersion = "1.6.1"
lazy val circeVersion = "0.11.1"
lazy val logbackVersion = "1.2.3"
lazy val avro4sVersion = "1.9.0"
lazy val avro4s2Version = "2.0.4"
lazy val json4sVersion = "3.6.6"
lazy val slf4jVersion = "1.7.25"
lazy val sprayJsonVersion = "1.3.5"
lazy val kafkaClientVersion = "2.2.1"
lazy val jsoninterScalaVersion = "0.28.1"
lazy val confluentPlatformVersion = "5.1.3"
lazy val scalaTestVersion = "3.0.7"
lazy val scalaCheckVersion = "1.14.0"
lazy val scalaMockVersion = "3.6.0"
lazy val wiremockVersion = "2.23.2"
lazy val scalaArmVersion = "2.0"

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
          Resolver.bintrayRepo("tpolecat", "maven"),
          "confluent-release" at "http://packages.confluent.io/maven/"
        ),
        bintrayOrganization := Some("ovotech"),
        bintrayRepository := "maven",
        bintrayPackageLabels := Seq(
          "apache-kafka",
          "serialization",
          "json",
          "avro",
          "circe",
          "spray-json",
          "json4s",
          "avro4s"
        ),
        releaseEarlyWith := BintrayPublisher,
        releaseEarlyNoGpg := true,
        releaseEarlyEnableSyncToMaven := false
      )
    )
  )
  .settings(name := "kafka-serialization", publishArtifact := false, publish := {})

lazy val doc = project
  .in(file("doc"))
  .enablePlugins(TutPlugin)
  .dependsOn(
    avro % Tut,
    avro4s % Tut,
    cats % Tut,
    circe % Tut,
    core % Tut,
    json4s % Tut,
    `jsoniter-scala` % Tut,
    spray % Tut
  )
  .settings(
    name := "kafka-serialization-doc",
    publishArtifact := false,
    publish := {},
    tutTargetDirectory := (baseDirectory.value).getParentFile,
    libraryDependencies ++= Seq("io.circe" %% "circe-generic" % circeVersion)
  )

lazy val testkit = project
  .in(file("testkit"))
  .settings(
    name := "kafka-serialization-testkit",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion,
      "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion,
      "com.github.tomakehurst" % "wiremock" % wiremockVersion,
      "com.jsuereth" %% "scala-arm" % scalaArmVersion,
      "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
      "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
      "ch.qos.logback" % "logback-core" % logbackVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    )
  )

lazy val json4s = project
  .in(file("json4s"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-json4s",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.json4s" %% "json4s-core" % json4sVersion,
      "org.json4s" %% "json4s-native" % json4sVersion,
    )
  )

lazy val avro = project
  .in(file("avro"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-avro",
    libraryDependencies ++= Seq(
      "io.confluent" % "kafka-avro-serializer" % confluentPlatformVersion exclude ("org.slf4j", "slf4j-log4j12")
    )
  )

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

lazy val `jsoniter-scala` = project
  .in(file("jsoniter-scala"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-jsoniter-scala",
    libraryDependencies ++= Seq("com.github.plokhotnyuk.jsoniter-scala" %% "macros" % jsoninterScalaVersion)
  )

lazy val circe = project
  .in(file("circe"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion % Test,
    )
  )

lazy val spray = project
  .in(file("spray"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-spray",
    libraryDependencies ++= Seq("io.spray" %% "spray-json" % sprayJsonVersion)
  )

lazy val core = project
  .in(file("core"))
  .dependsOn(testkit % Test)
  .settings(
    name := "kafka-serialization-core",
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % kafkaClientVersion exclude ("org.slf4j", "slf4j-log4j12"),
      "org.slf4j" % "slf4j-api" % slf4jVersion,
    )
  )

lazy val cats = project
  .in(file("cats"))
  .dependsOn(core, testkit % Test)
  .settings(
    name := "kafka-serialization-cats",
    libraryDependencies ++= Seq("org.typelevel" %% "cats-core" % catsVersion)
  )
