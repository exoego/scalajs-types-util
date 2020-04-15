name := "scalajs-util-types"

version := "0.1"

lazy val compilerSettings: Seq[Def.Setting[_]] = Seq(
  scalacOptions ++= Seq("-deprecation"),
  scalacOptions ++= Seq("-P:scalajs:sjsDefinedByDefault").filter(_ => scalaJSVersion.startsWith("0.6."))
)

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
    compilerSettings,
    metaMacroSettings,
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "scalameta" % "4.3.8",
      "org.scalatest" %%% "scalatest" % "3.1.1" % Test
    )
  )
  .enablePlugins(ScalaJSPlugin)
