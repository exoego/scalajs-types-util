val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).filter(_.nonEmpty).getOrElse("1.0.1")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"  % scalaJSVersion)
addSbtPlugin("com.dwijnand"  % "sbt-travisci" % "1.2.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
