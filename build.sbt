
val okHttpVersion = "3.11.0"
val avro4sVersion = "1.8.3"
val minimalJasonVersion = "0.9.5"

lazy val `kafka-serialization` = project
  .in(file("."))
  .aggregate(avro, avroJersey2, avro4s, avroOkHttp, cats, circe, core, json4s, `jsoniter-scala`, spray, testkit, doc)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization")
  .settings(Bintray.settings: _*)

lazy val doc = project
  .in(file("doc"))
  .dependsOn(
    avro % "tut",
    avroJersey2 % "tut",
    avroOkHttp % "tut",
    avro4s % "tut",
    cats % "tut",
    circe % "tut",
    core % "tut",
    json4s % "tut",
    `jsoniter-scala` % "tut",
    spray % "tut"
  )
  .enablePlugins(TutPlugin)
  .settings(Shared.settings: _*)
  .settings(
    name := "kafka-serialization-doc",
    publishArtifact := false,
    publish := {},
    tutTargetDirectory := baseDirectory.value.getParentFile
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

lazy val avroJersey2 = project
  .dependsOn(avro % "test->test;compile->compile")
  .in(file("avro-jersey-2"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-avro-jersey-2")
  .settings(Dependencies.avro)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)

lazy val avroOkHttp = project
  .dependsOn(avro % "test->test;compile->compile")
  .in(file("avro-okhttp"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-avro-okhttp")
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % okHttpVersion,
      "com.eclipsesource.minimal-json" % "minimal-json" % minimalJasonVersion,
    )
  )


lazy val avro4s = project
  .in(file("avro4s"))
  .dependsOn(core, avro, avroOkHttp % Test, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-avro4s")
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion
    )
  )

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

lazy val cats = project
  .in(file("cats"))
  .dependsOn(core, testkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(Shared.settings: _*)
  .settings(name := "kafka-serialization-cats")
  .settings(Dependencies.cats)
  .settings(Bintray.settings: _*)
  .settings(Git.settings: _*)
