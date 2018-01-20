import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}

lazy val `kafka-serialization` = project
  .in(file("."))
  .aggregate(avro, avro4s, circe, core, json4s, `jsoniter-scala`, spray, testkit, doc)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization")
  .settings(Bintray.settings: _*)

lazy val doc = project
  .in(file("doc"))
  .dependsOn(avro % "tut", avro4s % "tut", circe % "tut", core % "tut", json4s % "tut", `jsoniter-scala` % "tut", spray % "tut")
  .enablePlugins(TutPlugin)
  .settings(Shared.settings: _*)
  .settings(
    name := "kafka-serialization-doc",
    publishArtifact := false,
    publish := {},
    tutTargetDirectory := (baseDirectory.value).getParentFile
  )
  .settings(Dependencies.doc)

lazy val testkit = project
  .in(file("testkit"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-testkit", publishArtifact := false, publish := {})
  .settings(Dependencies.testkit)
  .settings(Git.settings: _*)

lazy val json4s = project
  .in(file("json4s"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-json4s")
  .settings(Dependencies.json4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(Bintray.settings: _*)
  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

lazy val avro = project
  .in(file("avro"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-avro")
  .settings(Dependencies.avro)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val avro4s = project
  .in(file("avro4s"))
  .dependsOn(core, avro, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-avro4s")
  .settings(Dependencies.avro4s)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val `jsoniter-scala` = project
  .in(file("jsoniter-scala"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-jsoniter-scala")
  .settings(Dependencies.jsoniterScala)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val circe = project
  .in(file("circe"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-circe")
  .settings(Dependencies.circe)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val spray = project
  .in(file("spray"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-spray")
  .settings(Dependencies.spray)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val core = project
  .in(file("core"))
  .dependsOn(testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-core")
  .settings(Dependencies.core)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
