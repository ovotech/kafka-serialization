import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}


lazy val `kafka-serialization` = project
  .in(file("."))
  .aggregate(avro, avro4s, circe, core, json4s, spray, testkit, doc)
  .enablePlugins(GitVersioning, GitBranchPrompt, TutPlugin)
  .settings(
    name := "kafka-serialization"
  )
  .settings(Shared.settings: _*)

lazy val doc = project
  .in(file("doc"))
  .dependsOn(avro % "tut", avro4s % "tut", circe % "tut", core % "tut", json4s % "tut", spray % "tut")
  .enablePlugins(TutPlugin)
  .settings(Shared.settings: _*)
  .settings(
    name := "kafka-serialization-doc",
    publishTo := None,
    tutTargetDirectory := (baseDirectory.value).getParentFile
  )
  .settings(Dependencies.doc)

lazy val testkit = project
  .in(file("testkit"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-testkit"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.testkit)
  .settings(Git.settings: _*)

lazy val json4s = project
  .in(file("json4s"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-json4s"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.json4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

lazy val avro = project
  .in(file("avro"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-avro"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.avro)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val avro4s = project
  .in(file("avro4s"))
  .dependsOn(core, avro, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-avro4s"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.avro4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val circe = project
  .in(file("circe"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-circe"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.circe)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val spray = project
  .in(file("spray"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-spray"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.spray)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val core = project
  .in(file("core"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "kafka-serialization-core"
  )
  .settings(Shared.settings: _*)
  .settings(Dependencies.core)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

