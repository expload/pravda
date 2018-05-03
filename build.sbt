val commonSettings = Seq(
  organization := "io.mytc",
  crossScalaVersions := Seq("2.12.4"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Xlint",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import"
  )
)

lazy val vmVersion = "0.0.1"
lazy val vmApi = (project in file("vm-api")).
  settings(
    normalizedName := "sood-api",
    version := vmVersion
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "org.scalatest"  %% "scalatest"        % "3.0.5"   % Test
    )
  )

lazy val vm = (project in file("vm")).
  settings(
    normalizedName := "sood",
    version := vmVersion
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "org.scodec" %% "scodec-bits" % "1.1.5",
      "org.scalatest"  %% "scalatest"        % "3.0.5"   % Test
    )
  ).
  aggregate(vmApi).
  dependsOn(vmApi)

lazy val asm = (project in file("asm")).
  dependsOn(vmApi).
  settings(
    normalizedName := "asm",
    version := "0.0.1"
  ).
  enablePlugins(JavaAppPackaging).
  settings( commonSettings: _* ).
  settings( mainClass in Compile := Some("io.mytc.sood.asm.Application") ).
  settings(
    libraryDependencies ++= Seq (
      "com.lihaoyi"    %% "fastparse"  % "1.0.0",
      "org.scalatest"  %% "scalatest"  % "3.0.5"   % Test
    )
  )

lazy val forth = (project in file("forth")).
  settings(
    normalizedName := "forth",
    version := "0.0.1"
  ).
  dependsOn(asm).
  enablePlugins(JavaAppPackaging).
  settings( commonSettings: _* ).
  settings( mainClass in Compile := Some("io.mytc.sood.forth.Application") ).
  settings(
    libraryDependencies ++= Seq (
      "com.lihaoyi"    %% "fastparse"  % "1.0.0",
      "org.scalatest"  %% "scalatest"  % "3.0.5"   % Test
    )
  )

lazy val tests = (project in file("tests")).
  dependsOn(vm).
  dependsOn(asm).
  dependsOn(forth).
  settings(
    normalizedName := "tests",
    version := "0.0.1"
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "org.scalatest"  %% "scalatest"  % "3.0.5"   % Test
    )
  )

lazy val cil = (project in file("cil-translator")).
  dependsOn(asm).
  settings(
    normalizedName := "cil-translator",
    version := "0.0.1"
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "com.lihaoyi" %% "fastparse-byte" % "1.0.0",
      "org.scalatest"  %% "scalatest"        % "3.0.5"   % Test
    )
  )
