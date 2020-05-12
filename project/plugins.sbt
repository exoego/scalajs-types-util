val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.33")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"  % scalaJSVersion)
addSbtPlugin("com.dwijnand"  % "sbt-travisci" % "1.2.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.4")
