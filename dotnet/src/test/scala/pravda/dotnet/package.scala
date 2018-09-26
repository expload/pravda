package pravda

import java.nio.file.{Files, Paths}

import pravda.dotnet.data.{Method, TablesData}
import pravda.dotnet.parser.CIL.CilData
import pravda.dotnet.parser.PE.Info.{Pdb, Pe}
import pravda.dotnet.parser.{FileParser, Signatures}

package object dotnet {

  private def readResourceBytes(filename: String) =
    Files.readAllBytes(Paths.get(s"dotnet-tests/resources/$filename"))

  def parsePeFile(file: String): Either[String, (Pe, CilData, List[Method], Map[Long, Signatures.Signature])] = {
    val fileBytes = readResourceBytes(file)
    FileParser.parsePe(fileBytes)
  }

  def parsePdbFile(file: String): Either[String, (Pdb, TablesData)] = {
    val fileBytes = readResourceBytes(file)
    FileParser.parsePdb(fileBytes)
  }
}
