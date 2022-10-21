addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.8.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.2.21")
addSbtPlugin("fr.qux" % "sbt-release-tags-only" % "0.5.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addDependencyTreePlugin

// TO mute sbt-git
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.30"
