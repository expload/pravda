package pravda

import java.nio.file.{Files, Paths}

import pravda.dotnet.data.Method
import pravda.dotnet.parsers.CIL.CilData
import pravda.dotnet.parsers.PE.Info.Pe
import pravda.dotnet.parsers.{FileParser, Signatures}

package object dotnet {

  def parseFile(file: String): Either[String, (Pe, CilData, List[Method], Map[Long, Signatures.Signature])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    FileParser.parsePe(fileBytes)
  }
}
