name := "scalajs-util-types"

version := "0.1"

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  Compile / scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
      case _                       => Nil
    }
  },
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => Nil
      case _                       => compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
    }
  }
)

lazy val root = project
  .in(file("."))
  .aggregate(macros)

lazy val macros = project
  .in(file("macros"))
  .settings(
    metaMacroSettings,
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "scalameta" % "4.3.0",
      "org.scalatest" %%% "scalatest" % "3.1.0" % Test
    )
  )
  .enablePlugins(ScalaJSPlugin)
