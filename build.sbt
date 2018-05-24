import java.nio.file.Files

resolvers += "jitpack" at "https://jitpack.io"

val commonSettings = Seq(
  organization := "io.mytc",
  version := "0.1.0",
  crossScalaVersions := Seq("2.12.4"),
  libraryDependencies ++= Seq(
    // Tests
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
    "-unchecked"
  )
) ++ scalafixSettings

lazy val common = (project in file("common")).
  settings(
    normalizedName := "pravda-common"
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "com.google.protobuf" % "protobuf-java" % "3.5.0",
      "com.propensive" %% "contextual" % "1.1.0",
      "org.whispersystems" % "curve25519-java" % "0.4.1"
    )
  )

lazy val vmApi = (project in file("vm-api")).
  settings(
    normalizedName := "pravda-vm-api",
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "com.google.protobuf" % "protobuf-java" % "3.5.0"
    )
  ).
  dependsOn(common)

lazy val vm = (project in file("vm")).
  settings(normalizedName := "pravda-vm").
  settings( commonSettings: _* ).
	dependsOn(vmApi).
  dependsOn(common)

lazy val `vm-asm` = (project in file("vm-asm")).
  dependsOn(vmApi).
  settings(normalizedName := "pravda-vm-asm").
  settings( commonSettings: _* ).
  settings( mainClass in Compile := Some("pravda.vm.asm.Application") ).
  settings(
    libraryDependencies ++= Seq (
      "com.github.scopt" %% "scopt"      % "3.7.0",
      "com.lihaoyi"      %% "fastparse"  % "1.0.0"
    )
  )

lazy val forth = (project in file("forth")).
  settings(normalizedName := "pravda-forth").
  dependsOn(`vm-asm`).
  settings( commonSettings: _* ).
  settings( mainClass in Compile := Some("pravda.forth.Application") ).
  settings(
    libraryDependencies ++= Seq (
      "com.github.scopt" %% "scopt"      % "3.7.0",
      "com.lihaoyi"      %% "fastparse"  % "1.0.0"
    )
  )

lazy val `forth-vm-integration-test` = (project in file("testkit/forth-vm-integration")).
  dependsOn(vm).
  dependsOn(`vm-asm`).
  dependsOn(forth).
  settings(normalizedName := "pravda-testkit-forth-vm-integration").
  settings( commonSettings: _* )

lazy val cil = (project in file("cil-translator")).
  dependsOn(`vm-asm`).
  settings(
    normalizedName := "cil-translator",
    version := "0.0.1"
  ).
  settings( commonSettings: _* ).
  settings(
    libraryDependencies ++= Seq (
      "com.lihaoyi" %% "fastparse-byte" % "1.0.0"
    )
  )


val `tendermint-version` = "0.16.0"

lazy val `node-db` = (project in file("node-db"))
  .disablePlugins(RevolverPlugin)
  .settings(
    normalizedName := "pravda-node-db",
    libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.10"
  )

lazy val node = (project in file("node"))
	.enablePlugins(JavaAppPackaging)
	.settings( commonSettings: _* )
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
		  "org.rudogma" %% "supertagged" % "1.4",
		  "io.mytc" %% "scala-abci-server" % "0.9.2",
		  "com.github.pureconfig" %% "pureconfig" % "0.9.0",
		  "org.typelevel" %% "cats-core" % "1.0.1",
		  // Marshalling
		  "com.tethys-json" %% "tethys" % "0.6.2",
		  "org.json4s" %%	"json4s-ast" % "3.5.3",
		  "io.suzaku" %% "boopickle" % "1.2.6",
		  "com.lightbend.akka"    %% "akka-stream-alpakka-unix-domain-socket" % "0.17",
		  "name.pellet.jp" %% "bsonpickle" % "0.4.4.2",
		  "com.chuusai" %% "shapeless" % "2.3.3"
		),
		scalacOptions ++= Seq(
		  "-Xmacro-settings:materialize-derivations"
		  , "-Ypartial-unification"
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
					val url = s"https://github.com/tendermint/tendermint/releases/download/v$version/tendermint_${version}_$ghSuffix.zip"
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
  .dependsOn(forth)

lazy val cli = (project in file("cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(
		normalizedName := "pravda",
    libraryDependencies ++= Seq (
      "com.github.scopt" %% "scopt" % "3.7.0",
      "org.typelevel" %% "cats-core" % "1.0.1",
    )
  )
  .dependsOn(common)
  .dependsOn(forth)
  .dependsOn(`vm-asm`)
  .dependsOn(vm)
  .dependsOn(node)

