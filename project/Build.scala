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

  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less")
  )

  def customJSEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "javascripts" / "bootstrap" * "*.js") +++
    (base / "app" / "assets" / "javascripts" * "*.js")
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    scalaVersion := "2.9.1",
    ensimeConfig := sexp(
      key(":only-include-in-index"), sexp(
        "controllers\\..*",
        "models\\..*",
        "views\\..*",
        "play\\..*"
      )
    ),
    lessEntryPoints <<= baseDirectory(customLessEntryPoints),
    javascriptEntryPoints <<= baseDirectory(customJSEntryPoints)
  )
}
