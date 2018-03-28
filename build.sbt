import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.mytc",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "sood",
    libraryDependencies += scalaTest % Test
  )
