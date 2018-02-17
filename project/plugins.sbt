addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "4.1.0")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.2")

addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.2")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.15")

resolvers += Resolver.bintrayRepo("tpolecat", "maven")

// TO mute sbt-git
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.22"
