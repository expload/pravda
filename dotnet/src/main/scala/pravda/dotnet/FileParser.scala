package pravda.dotnet

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import pravda.dotnet.CIL.CilData
import pravda.dotnet.PE.Info.Pe
import pravda.dotnet.utils._

object FileParser {

  def parseFile(file: String): Validated[(Pe, CilData, Seq[Method], Map[Long, Signatures.Signature])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    parsePe(fileBytes)
  }

  def parsePe(bytes: Array[Byte]): Validated[(Pe, CilData, Seq[Method], Map[Long, Signatures.Signature])] = {
    val peV = PE.parseInfo(Bytes(bytes))

    for {
      pe <- peV
      cilData <- CIL.fromPeData(pe.peData)
      methods <- pe.methods.map(Method.parse(pe.peData, _)).sequence
      signatures <- Signatures.collectSignatures(cilData)
    } yield (pe, cilData, methods, signatures)
  }
}
