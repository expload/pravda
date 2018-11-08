import java.nio.file.Files

resolvers += "jitpack" at "https://jitpack.io"
resolvers += Resolver.bintrayRepo("expload", "oss")

enablePlugins(GitVersioning)

scalaVersion := "2.12.6"
skip in publish := true
headerLicense := Some(HeaderLicense.AGPLv3("2018", "Expload"))
git.useGitDescribe := true
git.gitTagToVersionNumber := { tag: String =>
  if (tag.length > 0) Some(tag.stripPrefix("v"))
  else None
}

val `tendermint-version` = "0.16.0"

lazy val envDockerUsername = sys.env.get("docker_username")

val scalacheckOps = Seq(
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  testOptions in Test ++= Seq(
    Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "3"),
    Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1"),
    Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "100")
  )
)

val commonSettings = Seq(
  organization := "com.expload",
  licenses += ("AGPL-V3", url("http://www.opensource.org/licenses/agpl-v3.html")),
  headerLicense := Some(HeaderLicense.AGPLv3("2018", "Expload.com")),
  excludeFilter.in(headerSources) := HiddenFileFilter || "ed25519.java" || "ripemd160.java",
  skip in publish := false,
  bintrayOrganization := Some("expload"),
  bintrayRepository := "oss",
  bintrayVcsUrl := Some("https://github.com/expload/pravda"),
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
    "-Ypartial-unification",
    "-Ypatmat-exhaust-depth",
    "40"
  ),
  resolvers += "jitpack" at "https://jitpack.io",
  resolvers += Resolver.bintrayRepo("expload", "oss")
) // ++ scalafixSettings

val dotnetTests = file("dotnet-tests/resources")

lazy val common = (project in file("common"))
  .settings(commonSettings: _*)
  .settings(
    name := "pravda-common",
    normalizedName := "pravda-common",
    description := "Common utils used across Pravda"
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % "3.5.0",
      "com.propensive" %% "contextual" % "1.1.0",
      "org.whispersystems" % "curve25519-java" % "0.4.1",
      "org.rudogma" %% "supertagged" % "1.4",
      "com.tethys-json" %% "tethys" % "0.7.0.2",
      "com.tethys-json" %% "tethys-derivation" % "0.7.0.2",
      "com.tethys-json" %% "tethys-json4s" % "0.7.0.2",
      "org.json4s" %% "json4s-ast" % "3.6.1"
    )
  )

lazy val `vm-api` = (project in file("vm-api"))
  .settings(commonSettings: _*)
  .settings(
    name := "pravda-vm-api",
    normalizedName := "pravda-vm-api",
    description := "Pravda VM API"
  )
  .settings(scalacheckOps: _*)
  .settings(
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "1000")
    ),
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % "3.5.0",
      "com.lihaoyi" %% "fastparse" % "1.0.0",
      "com.tethys-json" %% "tethys" % "0.7.0.2"
    )
  )
  .dependsOn(common)

lazy val vm = (project in file("vm"))
  .settings(commonSettings: _*)
  .settings(
    name := "pravda-vm",
    normalizedName := "pravda-vm",
    description := "Pravda Virtual Machine",
    sources in doc := Seq.empty,
    publishArtifact in packageDoc := false,
  )
  .settings(
    sources in doc := Seq.empty,
    publishArtifact in packageDoc := false,
    testFrameworks := Seq(new TestFramework("pravda.common.PreserveColoursFramework"))
  )
  .dependsOn(`vm-api`, `vm-asm` % "compile->test")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(plaintest % "compile->test")

lazy val `vm-asm` = (project in file("vm-asm"))
  .settings(commonSettings: _*)
  .settings(
    name := "pravda-vm-asm",
    normalizedName := "pravda-vm-asm",
    description := "Pravda Virtual Machine Assembly language"
  )
  .settings(scalacheckOps: _*)
  .settings(
    testOptions in Test ++= Seq(
      // Reduce size because PravdaAssemblerSpecification
      // generated too big operation lists.
      Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "7"),
    )
  )
  .dependsOn(`vm-api` % "test->test;compile->compile")

lazy val dotnet = (project in file("dotnet"))
  .settings(commonSettings: _*)
  .settings(
    name := "pravda-dotnet",
    normalizedName := "pravda-dotnet",
    description := "Pravda .NET-compatible languages",
    unmanagedResourceDirectories in Test += dotnetTests
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.1",
      "com.lihaoyi" %% "fastparse-byte" % "1.0.0",
      "com.lihaoyi" %% "pprint" % "0.5.3" % "test"
    ),
    testFrameworks := Seq(new TestFramework("pravda.common.PreserveColoursFramework"))
  )
  .dependsOn(`vm-asm`)
  .dependsOn(common % "test->test")
  .dependsOn(plaintest % "compile->test")

lazy val `node-db` = (project in file("node-db"))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "pravda-node-db",
    normalizedName := "pravda-node-db",
    description := "Pravda Node Database"
  )
  .settings(
    normalizedName := "pravda-node-db",
    libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.10"
  )

lazy val node = (project in file("node"))
  .enablePlugins(UniversalPlugin)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(scalacheckOps:_*)
  .settings(
    name := "pravda-node",
    normalizedName := "pravda-node",
    description := "Pravda network node",
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "pravda.node",
  )
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
      "com.expload" %% "scala-abci-server" % "0.12.0",
      "com.github.pureconfig" %% "pureconfig" % "0.9.1",
      // Marshalling
      "com.tethys-json" %% "tethys" % "0.7.0.2",
      "com.lightbend.akka" %% "akka-stream-alpakka-unix-domain-socket" % "0.17",
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    dependencyOverrides += "org.scala-lan" %% "scala-compiler" % "2.12.6",
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
    skip in publish := true,
    normalizedName := "yopt"
  )

lazy val codegen = (project in file("codegen"))
  .settings(commonSettings: _*)
  .settings(normalizedName := "pravda-codegen")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.1",
      "com.github.spullara.mustache.java" % "compiler" % "0.9.5"
    ))
  .settings(scalacOptions ++= Seq(
    "-Ypartial-unification"
  ))
  .dependsOn(`vm-asm`)
  .dependsOn(common % "test->test")

lazy val `node-client` = (project in file("node-client"))
  .enablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)
  .settings(scalacheckOps:_*)
  .settings(
    name := "pravda-node-client",
    normalizedName := "pravda-node-client",
    description := "Pravda node client",
  )
  .dependsOn(common)
  .dependsOn(vm)
  .dependsOn(node)
  .dependsOn(codegen)
  .dependsOn(dotnet)

// A service for build, sign and broadcast transactions
// within authorized environment
lazy val `broadcaster` = (project in file("services/broadcaster"))
  .enablePlugins(UniversalPlugin)
  .enablePlugins(ClasspathJarPlugin)
  .settings(commonSettings: _*)
  .settings(scalacheckOps:_*)
  .settings(normalizedName := "pravda-broadcaster")
  .dependsOn(`node-client`)

lazy val cli = (project in file("cli"))
  .enablePlugins(ClasspathJarPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "pravda.cli",
    skip in publish := true,
    normalizedName := "pravda",
    mainClass in Compile := Some("pravda.cli.Pravda"),
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.7.0",
      "org.typelevel" %% "cats-core" % "1.0.1",
    ),
    bashScriptExtraDefines += """set -- -- "$@""""
  )
  .dependsOn(yopt)
  .dependsOn(`node-client` % "compile->compile;test->test")

lazy val `gen-doc` = (project in file("doc") / "gen")
  .settings(commonSettings: _*)
  .settings(
    skip in publish := true,
    normalizedName := "pravda-gen-doc",
    headerLicense := Some(HeaderLicense.AGPLv3("2018", "Expload"))
  )
  .dependsOn(cli)
  .dependsOn(vm)
  .dependsOn(`vm-asm`)

lazy val testkit = (project in file("testkit"))
  .settings(commonSettings: _*)
  .settings(
    skip in publish := true,
    normalizedName := "pravda-testkit",
    unmanagedResourceDirectories in Test += dotnetTests,
    testFrameworks := Seq(new TestFramework("pravda.common.PreserveColoursFramework"))
  )
  .dependsOn(common % "test->test")
  .dependsOn(vm % "compile->compile;test->test")
  .dependsOn(`vm-api`)
  .dependsOn(`vm-asm`)
  .dependsOn(dotnet % "compile->compile;test->test")
  .dependsOn(codegen)

lazy val yaml4s = (project in file("yaml4s"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % "1.23",
      "org.json4s" %% "json4s-native" % "3.6.1"
    )
  )

lazy val plaintest = (project in file("plaintest"))
  .dependsOn(yaml4s)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.6.3"
    )
  )

lazy val `xcoins-receiving` = (project in file("services/xcoins-receiving"))
  .enablePlugins(ClasspathJarPlugin)
  .dependsOn(common)
  .dependsOn(`node-client`)
  .settings(commonSettings: _*)
  .settings(
    skip in publish := true,
    normalizedName := "pravda-services-xcoins-receiving",
    libraryDependencies ++= Seq(
      // Networking
      "com.typesafe.akka" %% "akka-actor" % "2.5.8",
      "com.typesafe.akka" %% "akka-stream" % "2.5.8",
      "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
      // UI
      "com.github.fomkin" %% "korolev-server-akkahttp" % "0.7.0"
    )
  )
