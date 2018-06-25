package pravda.cli.docs

import java.io.File

import pravda.cmdopt.CommandLine

final case class GenDocsConfig(outDir: String = "docs", mainPageName: String = "main.md")

object GenDocsArgsParser extends CommandLine[GenDocsConfig] {

  def model =
    List(
      head("gen-docs")
        .title("Generate documentation for Pravda Command line tool."),
      cmd("markdown")
        .text("Generate markdown documentation for command line tool.")
        .action(_ => GenDocsConfig())
        .children(
          head("pravda-gen-docs")
            .title("Generate markdown documentation.")
            .text("pravda gen docs")
            .desc("Generate and write to a given folder (docs/ref/ by default) comprehensive markdown docs for CLI."),
          opt[File]('o', "output")
            .text("Output directory")
            .action {
              case (file, GenDocsConfig(outDir, mainPageName)) =>
                GenDocsConfig(file.getAbsolutePath, mainPageName)
              case (_, otherwise) => otherwise
            }
        )
    )
}
