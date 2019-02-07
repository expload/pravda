package pravda
import pravda.dotnet.parser.FileParser

package object dotnet {

  def clearPathsInPdb(files: List[FileParser.ParsedDotnetFile]): List[FileParser.ParsedDotnetFile] = {
    def clear(s: String): String = "$PRAVDA_TMP_DIR/" + s"${s.split("/").last}"

    // Drop the first Pravda.cs file
    files.drop(1).map { df =>
      val clearedPdb = df.parsedPdb.map { pdb =>
        pdb.copy(
          tablesData = pdb.tablesData.copy(
            methodDebugInformationTable =
              pdb.tablesData.methodDebugInformationTable.map(d => d.copy(document = d.document.map(clear))),
            documentTable = pdb.tablesData.documentTable.map(d => d.copy(path = clear(d.path)))
          )
        )
      }

      df.copy(parsedPdb = clearedPdb)
    }
  }

}
