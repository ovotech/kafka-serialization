import bintray.BintrayPlugin.autoImport.{bintrayOrganization, bintrayPackageLabels, bintrayRepository}

object Bintray {

  lazy val settings = Seq(
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("kafka", "serialization"))

}