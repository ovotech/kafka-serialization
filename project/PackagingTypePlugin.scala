import sbt._

/**
  * @see https://github.com/sbt/sbt/issues/3618#issuecomment-424924293
  */
object PackagingTypePlugin extends AutoPlugin {
  override val buildSettings: Seq[Setting[_]] = {
    sys.props += "packaging.type" -> "jar"
    Nil
  }
}
