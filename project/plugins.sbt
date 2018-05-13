addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")

addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.4")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.15")

resolvers += Resolver.bintrayRepo("tpolecat", "maven")

// TO mute sbt-git
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
