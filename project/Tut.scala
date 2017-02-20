import sbt.Keys._
import tut.Plugin._

object Tut {

  lazy val settings = tutSettings ++ Seq(
    tutTargetDirectory := baseDirectory.value
  )

}