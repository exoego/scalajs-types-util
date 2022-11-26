val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).filter(_.nonEmpty).getOrElse("1.11.0")
addSbtPlugin("org.scala-js"   % "sbt-scalajs"  % scalaJSVersion)
addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.2.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.15")
