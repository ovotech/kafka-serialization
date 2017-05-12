addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.6.0")
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0-8-g6d0c3f8")
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.5.1")

// To resolve custom bintray-sbt plugin
resolvers += Resolver.url("2m-sbt-plugin-releases", url("https://dl.bintray.com/2m/sbt-plugin-releases/"))(
  Resolver.ivyStylePatterns
)

resolvers += Resolver.bintrayRepo("tpolecat", "maven")

// TO mute sbt-git
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.22"
