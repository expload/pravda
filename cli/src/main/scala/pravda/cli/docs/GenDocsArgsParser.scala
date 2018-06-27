package pravda.cli.docs

import java.io.File

import pravda.yopt.CommandLine

final case class GenDocsConfig(outDir: String = "docs", mainPageName: String = "main.md")

object GenDocsArgsParser extends CommandLine[GenDocsConfig] {

  val model =
      head("gen-docs")
        .text("Generate documentation for Pravda Command line tool.")
        .children(
          opt[File]('o', "output")
            .text("Output directory")
            .action {
              case (file, GenDocsConfig(outDir, mainPageName)) =>
                GenDocsConfig(file.getAbsolutePath, mainPageName)
              case (_, otherwise) => otherwise
            }
    )
}
