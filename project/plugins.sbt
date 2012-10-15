// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Typesafe Snapshots repository" at "http://repo.typesafe.com/typesafe/snapshots/"

resolvers += Resolver.url("Typesafe Ivy Snapshot repository", new java.net.URL("http://repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns)

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % Option(System.getProperty("play.version")).getOrElse("2.1-SNAPSHOT"))

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")
