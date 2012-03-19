import sbt._
import Keys._
import PlayProject._

import org.ensime.sbt.Plugin.Settings.ensimeConfig
import org.ensime.sbt.util.SExp._

object ApplicationBuild extends Build {
  val appName         = "Performance reporter"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "postgresql" % "postgresql" % "8.4-702.jdbc4"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    ensimeConfig := sexp(
      key(":only-include-in-index"), sexp(
        "controllers\\..*",
        "models\\..*",
        "views\\..*",
        "play\\..*"
      )
    )
  )
}
