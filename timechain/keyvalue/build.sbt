organization := "io.mytc"
scalaVersion := "2.12.4"
version      := "0.1.0-SNAPSHOT"
name := "keyvalue"

disablePlugins(RevolverPlugin)

libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.10"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
