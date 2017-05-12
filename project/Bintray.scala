import bintray.BintrayKeys.{bintrayOrganization, bintrayPackageLabels, bintrayRepository}

object Bintray {

  lazy val settings = Seq(
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("apache-kafka", "serialization", "json", "avro", "circe", "spray-json", "json4s", "avro4s")
  )
}