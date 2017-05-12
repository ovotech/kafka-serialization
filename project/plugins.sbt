addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.6.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.4.0")
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.5.1")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "0.3")

// To resolve custom bintray-sbt plugin
resolvers += Resolver.url("2m-sbt-plugin-releases", url("https://dl.bintray.com/2m/sbt-plugin-releases/"))(
  Resolver.ivyStylePatterns
)

resolvers += Resolver.bintrayRepo("tpolecat", "maven")

// TO mute sbt-git
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.22"

// To solve the json4s issue with bintray
dependencyOverrides ++= Set(
  "org.json4s" %% "json4s-core" % "3.2.10",
  "org.json4s" %% "json4s-ast" % "3.2.10",
  "org.json4s" %% "json4s-native" % "3.2.10"
)