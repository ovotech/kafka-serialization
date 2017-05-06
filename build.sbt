import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}

lazy val `kafka-serialization` = project
  .in(file("."))
  .settings(
    name := "kafka-serialization"
  )
  .settings(Shared.settings: _*)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .aggregate(avro, avro4s, circe, core, json4s, spray, testkit)

lazy val testkit = project
  .in(file("testkit"))
  .settings(
    name := "kafka-serialization-testkit"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.testkit)
  .settings(Git.settings: _*)

lazy val json4s = project
  .in(file("json4s"))
  .settings(
    name := "kafka-serialization-json4s"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.json4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
  .dependsOn(core, testkit % Test)

lazy val avro = project
  .in(file("avro"))
  .settings(
    name := "kafka-serialization-avro"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.avro)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(core, testkit % Test)

lazy val avro4s = project
  .in(file("avro4s"))
  .settings(
    name := "kafka-serialization-avro4s"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.avro4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(core, avro, testkit % Test)

lazy val circe = project
  .in(file("circe"))
  .settings(
    name := "kafka-serialization-circe"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.circe)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(core, testkit % Test)

lazy val spray = project
  .in(file("spray"))
  .settings(
    name := "kafka-serialization-spray"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.spray)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(core, testkit % Test)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "kafka-serialization-core"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.core)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(testkit % Test)
