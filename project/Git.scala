import com.typesafe.sbt.GitPlugin.autoImport.git
import com.typesafe.sbt.git.ConsoleGitRunner

object Git {

  lazy val settings = Seq(
    git.remoteRepo := "origin",
    git.runner := ConsoleGitRunner,
    git.baseVersion := "0.1.0",
    git.useGitDescribe := true
  )

}
