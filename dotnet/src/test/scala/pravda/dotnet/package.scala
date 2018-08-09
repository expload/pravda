package pravda

import java.nio.file.{Files, Paths}

import pravda.dotnet.data.{Method, TablesData}
import pravda.dotnet.parsers.CIL.CilData
import pravda.dotnet.parsers.PE.Info.{Pdb, Pe}
import pravda.dotnet.parsers.{FileParser, Signatures}

package object dotnet {

  private def readResourceBytes(filename: String) =
    Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$filename").getPath))

  def parsePeFile(file: String): Either[String, (Pe, CilData, List[Method], Map[Long, Signatures.Signature])] = {
    val fileBytes = readResourceBytes(file)
    FileParser.parsePe(fileBytes)
  }

  def parsePdbFile(file: String): Either[String, (Pdb, TablesData)] = {
    val fileBytes = readResourceBytes(file)
    FileParser.parsePdb(fileBytes)
  }
}
