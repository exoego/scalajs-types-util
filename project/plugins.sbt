val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).filter(_.nonEmpty).getOrElse("1.3.1")
addSbtPlugin("org.scala-js"   % "sbt-scalajs"  % scalaJSVersion)
addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("com.jsuereth"   % "sbt-pgp"      % "2.0.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")
