import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}

def OvoRootProject(name: String) = Project(name, file("."))

def OvoProject(name: String) = Project(name, file(name))

lazy val `kafka-serialization` = OvoRootProject("kafka-serialization")
  .settings(Shared.settings: _*)
  .settings(publish := {})
  .settings(Tut.settings: _*)
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .aggregate(`kafka-serialization-json4s`, `kafka-serialization-circe`, `kafka-serialization-avro4s`, `kafka-serialization-core`)

lazy val `kafka-serialization-json4s` = OvoProject("kafka-serialization-json4s")
  .settings(Shared.settings: _*)
  .settings(Dependencies.json4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
  .dependsOn(`kafka-serialization-core`, `kafka-serialization-core` % "test->test")

lazy val `kafka-serialization-avro4s` = OvoProject("kafka-serialization-avro4s")
  .settings(Shared.settings: _*)
  .settings(Dependencies.avro4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(`kafka-serialization-core`, `kafka-serialization-core` % "test->test")

lazy val `kafka-serialization-circe` = OvoProject("kafka-serialization-circe")
  .settings(Shared.settings: _*)
  .settings(Dependencies.circe)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .dependsOn(`kafka-serialization-core`, `kafka-serialization-core` % "test->test")

lazy val `kafka-serialization-core` = OvoProject("kafka-serialization-core")
  .settings(Shared.settings: _*)
  .settings(Dependencies.core)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)