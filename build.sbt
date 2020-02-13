name := "scalajs-util-types"

version := "0.1"

scalaVersion in ThisBuild := "2.13.1"

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  Compile / scalacOptions += "-Ymacro-annotations"
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
