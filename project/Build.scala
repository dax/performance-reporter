import sbt._
import Keys._
import play.Project._

import org.ensime.sbt.Plugin.Settings.ensimeConfig
import org.ensime.sbt.util.SExp._

object ApplicationBuild extends Build {
  val appName         = "Performance_reporter"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    jdbc,
    "postgresql" % "postgresql" % "8.4-702.jdbc4",
    "org.scalaz" %% "scalaz-core" % "6.0.4"
  )

  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less")
  )

  def customJSEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "javascripts" / "bootstrap" * "*.js") +++
    (base / "app" / "assets" / "javascripts" * "*.js")
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalaVersion := "2.10.0",
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
  ).dependsOn(RootProject( uri("git://github.com/freekh/play-slick.git") ))
}
