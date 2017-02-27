import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}

def OvoRootProject(name: String) = Project(name, file("."))

def OvoProject(name: String) = Project(name, file(name))

lazy val noPublish = Seq(
  publish := {},
  publishArtifact := false
)

lazy val `kafka-serialization-testkit` = OvoProject("kafka-serialization-testkit")
  .settings(Shared.settings: _*)
  .settings(Dependencies.testkit)
  .settings(noPublish: _*)
  .settings(Git.settings: _*)

lazy val `kafka-serialization` = OvoRootProject("kafka-serialization")
  .settings(Shared.settings: _*)
  .settings(noPublish: _*)
  .settings(Tut.settings: _*)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .aggregate(`kafka-serialization-avro4s`)
  .aggregate(`kafka-serialization-circe`)
  .aggregate(`kafka-serialization-client`)
  .aggregate(`kafka-serialization-core`)
  .aggregate(`kafka-serialization-json4s`)
  .aggregate(`kafka-serialization-testkit`)

lazy val `kafka-serialization-json4s` = OvoProject("kafka-serialization-json4s")
  .settings(Shared.settings: _*)
  .settings(Dependencies.json4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
  .dependsOn(`kafka-serialization-core`)
  .dependsOn(`kafka-serialization-testkit` % "test->test")

lazy val `kafka-serialization-avro4s` = OvoProject("kafka-serialization-avro4s")
  .settings(Shared.settings: _*)
  .settings(Dependencies.avro4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(`kafka-serialization-core`)
  .dependsOn(`kafka-serialization-testkit` % "test->test")

lazy val `kafka-serialization-circe` = OvoProject("kafka-serialization-circe")
  .settings(Shared.settings: _*)
  .settings(Dependencies.circe)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(`kafka-serialization-core`)
  .dependsOn(`kafka-serialization-testkit` % "test->test")

lazy val `kafka-serialization-core` = OvoProject("kafka-serialization-core")
  .settings(Shared.settings: _*)
  .settings(Dependencies.core)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(`kafka-serialization-testkit` % "test->test")

lazy val `kafka-serialization-client` = OvoProject("kafka-serialization-client")
  .settings(Shared.settings: _*)
  .settings(Dependencies.client)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(Defaults.itSettings: _*)
  .dependsOn(`kafka-serialization-core`)
  .dependsOn(`kafka-serialization-testkit` % "test->test")
  .dependsOn(`kafka-serialization-testkit` % "it->test")
  .dependsOn(`kafka-serialization-testkit` % "it->compile")
  .configs(IntegrationTest)