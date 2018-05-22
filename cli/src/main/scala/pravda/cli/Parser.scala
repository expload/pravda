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
          opt[File]('o', "output")
            .text("Output file")
            .action {
              case (file, Config.GenAddress(_)) =>
                Config.GenAddress(Config.Output.OsFile(file.getAbsolutePath))
              case (_, otherwise) => otherwise
            }
        )
    )

  cmd("test")
    .action((_, _) => Config.TestBytecode())
    .children(
      opt[File]('i', "input")
        .text("Input file")
        .action {
          case (file, config: Config.TestBytecode) =>
            config.copy(input = Config.Input.OsFile(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
      opt[File]("storage")
        .text("Storage name")
        .action {
          case (file, config: Config.TestBytecode) =>
            config.copy(storage = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        }
    )

}
