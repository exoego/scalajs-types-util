organization in ThisBuild := "net.exoego"
name in ThisBuild := "scalajs-types-util"
version in ThisBuild := "0.1"

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
    name := "scalajs-types-util",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    compilerSettings,
    metaMacroSettings,
    libraryDependencies ++= {
      val isSjs06 = Option(System.getenv("SCALAJS_VERSION")).filter(_.nonEmpty).exists(_.startsWith("0.6."))
      Seq(
        "org.scalameta" %%% "scalameta" % (if (isSjs06) "4.3.10" else "4.3.12"),
        "org.scalatest" %%% "scalatest" % "3.1.2" % Test
      )
    }
  )
  .enablePlugins(ScalaJSPlugin)
