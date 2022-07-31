import sbt._
import sbt.Keys._

val scala212Version = "2.12.16"
val scala213Version = "2.13.8"

ThisBuild / organization       := "net.exoego"
ThisBuild / scalaVersion       := scala213Version
ThisBuild / crossScalaVersions := Seq(scala212Version, scala213Version)

lazy val compilerSettings: Seq[Def.Setting[_]] = Seq(
  scalacOptions ++= Seq("-deprecation")
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
      case _ => compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
    }
  }
)

lazy val root = project
  .in(file("."))
  .aggregate(macros)
  .settings(name := "scalajs-types-util")

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
        "org.scalameta" %%% "scalameta" % (if (isSjs06) "4.3.10" else "4.5.11"),
        "org.scalatest" %%% "scalatest" % "3.2.13" % Test
      )
    },
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/exoego/scalajs-types-util"),
        "scm:git:git@github.com:exoego/scalajs-types-util.git"
      )
    ),
    homepage := scmInfo.value.map(_.browseUrl),
    developers := List(
      Developer(
        id = "exoego",
        name = "TATSUNO Yasuhiro",
        email = "ytatsuno.jp@gmail.com",
        url = url("https://www.exoego.net")
      )
    ),
    publishMavenStyle            := true,
    Test / publishArtifact       := false,
    packageSrc / publishArtifact := true,
    packageDoc / publishArtifact := true,
    pomIncludeRepository := { _ =>
      false
    },
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    publishConfiguration      := publishConfiguration.value.withOverwrite(false),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
  )
  .enablePlugins(ScalaJSPlugin)
