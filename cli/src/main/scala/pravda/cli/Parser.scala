package pravda.cli

import java.io.File

import scopt.OptionParser

object Parser extends OptionParser[Config]("pravda") {

  head("Pravda Command Line Interface")

  cmd("gen")
    .children(
      cmd("address")
        .action((_, _) => Config.GenAddress())
        .children(
          opt[File]('o', "output").action {
            case (file, Config.GenAddress(_)) =>
              Config.GenAddress(Config.Output.OsFile(file.getAbsolutePath))
            case (_, otherwise) => otherwise
          }
        )
    )
  //      opt[Unit]('g', "genkey")
  //        .action { (_, c) =>
  //          c.copy(generateKeyPair = true)
  //        }
  //        .text("Generate ED25519 key pair")
  //
  //      help("help").text("Unified CLI tool")

}
