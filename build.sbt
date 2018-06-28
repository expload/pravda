import java.nio.file.Files

resolvers += "jitpack" at "https://jitpack.io"

enablePlugins(GitVersioning)

git.gitTagToVersionNumber := { tag: String =>
  if (tag.length > 0) Some(tag)
  else None
}

val `tendermint-version` = "0.16.0"

val scalacheckOps = Seq(
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  testOptions in Test ++= Seq(
    Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "3"),
    Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1"),
    Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "100")
  )
)

val commonSettings = Seq(
  organization := "io.mytc",
  crossScalaVersions := Seq("2.12.4"),
  libraryDependencies ++= Seq(
    // Tests
    "org.typelevel" %% "cats-core" % "1.0.1",
    "org.rudogma" %% "supertagged" % "1.4",
    "com.lihaoyi" %% "utest" % "0.6.3" % "test"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Xlint",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-unchecked",
    "-Xmacro-settings:materialize-derivations",
    "-Ypartial-unification"
  )
) ++ scalafixSettings

lazy val common = (project in file("common"))
  .settings(
    normalizedName := "pravda-common"
  )
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % "3.5.0",
      "com.propensive" %% "contextual" % "1.1.0",
      "org.whispersystems" % "curve25519-java" % "0.4.1",
      "org.rudogma" %% "supertagged" % "1.4"
    )
  )

lazy val `vm-api` = (project in file("vm-api"))
  .settings(
    normalizedName := "pravda-vm-api",
  )
  .settings( commonSettings: _* )
  .settings(scalacheckOps:_*)
  .settings(
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "1000")
    ),
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % "3.5.0",
      "com.lihaoyi" %% "fastparse"  % "1.0.0"
    )
  )
  .dependsOn(common)

lazy val vm = (project in file("vm"))
  .settings(normalizedName := "pravda-vm")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.quicklens" %% "quicklens" % "1.4.11"
    )
  )
	.dependsOn(`vm-api`, `vm-asm` % "compile->test")
  .dependsOn(common)

lazy val `vm-asm` = (project in file("vm-asm"))
  .dependsOn(`vm-api` % "test->test;compile->compile")
  .settings(normalizedName := "pravda-vm-asm")
  .settings(commonSettings: _*)
  .settings(scalacheckOps:_*)
  .settings(
    testOptions in Test ++= Seq(
      // Reduce size because PravdaAssemblerSpecification
      // generated too big operation lists.
      Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "7"),
    ),
    libraryDependencies ++= Seq (
      "com.lihaoyi" %% "fastparse"  % "1.0.0"
    )
  )

lazy val dotnet = (project in file("dotnet"))
  .dependsOn(`vm-asm`)
  .settings(normalizedName := "pravda-dotnet")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.1",
      "com.lihaoyi" %% "fastparse-byte" % "1.0.0"
    )
  )

lazy val `node-db` = (project in file("node-db"))
  .disablePlugins(RevolverPlugin)
  .settings(
    normalizedName := "pravda-node-db",
    libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.10"
  )

lazy val node = (project in file("node"))
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(
    normalizedName := "pravda-node",
    libraryDependencies ++= Seq(
      // Networking
      "com.typesafe.akka" %% "akka-actor" % "2.5.8",
      "com.typesafe.akka" %% "akka-stream" % "2.5.8",
      "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
      // UI
      "com.github.fomkin" %% "korolev-server-akkahttp" % "0.7.0",
      // Other
      "io.mytc" %% "scala-abci-server" % "0.9.2",
      "com.github.pureconfig" %% "pureconfig" % "0.9.0",
      // Marshalling
      "com.tethys-json" %% "tethys" % "0.6.2",
      "org.json4s" %% "json4s-ast" % "3.5.3",
      "io.suzaku" %% "boopickle" % "1.2.6",
      "com.lightbend.akka" %% "akka-stream-alpakka-unix-domain-socket" % "0.17",
      "name.pellet.jp" %% "bsonpickle" % "0.4.4.2",
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    scalacOptions ++= Seq(
      "-Xmacro-settings:materialize-derivations",
      "-Ypartial-unification"
      //  , "-Xlog-implicits"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    // Download tendermint
    resourceGenerators in Compile += Def.task {
      def download(version: String, ghSuffix: String, resSuffix: String) = {
        val targetFile = (resourceManaged in Compile).value / s"tendermint_$resSuffix"
        if (!targetFile.exists()) {
          if (!targetFile.getParentFile.exists()) {
            targetFile.getParentFile.mkdirs()
          }
          val url =
            s"https://github.com/tendermint/tendermint/releases/download/v$version/tendermint_${version}_$ghSuffix.zip"
          sLog.value.info(s"downloading $url")
          val dir = Files.createTempDirectory("tmsbt").toFile
          val unzipped = IO.unzipURL(new URL(url), dir)
          Files.move(unzipped.head.toPath, targetFile.toPath).toFile
        } else {
          targetFile
        }
      }
      Seq(
        download(`tendermint-version`, "darwin_amd64", "macos_x86_64"),
        download(`tendermint-version`, "linux_amd64", "linux_x86_64"),
        download(`tendermint-version`, "windows_amd64", "windows_x86_64.exe")
      )
    }.taskValue,
    fork in run := true,
    connectInput in run := true,
    outputStrategy in run := Some(OutputStrategy.StdoutOutput)
  )
  .dependsOn(common)
	.dependsOn(`node-db`)
	.dependsOn(vm)
  .dependsOn(`vm-asm`)

lazy val yopt = (project in file("yopt"))
  .settings(commonSettings: _*)
  .settings(
    normalizedName := "yopt"
  )

lazy val cli = (project in file("cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(
    normalizedName := "pravda",
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.7.0",
      "org.typelevel" %% "cats-core" % "1.0.1",
    ),
    bashScriptExtraDefines += """set -- -- "$@""""
  )
  .dependsOn(yopt)
  .dependsOn(common)
  .dependsOn(`vm-asm`)
  .dependsOn(vm)
  .dependsOn(node)
  .dependsOn(dotnet)