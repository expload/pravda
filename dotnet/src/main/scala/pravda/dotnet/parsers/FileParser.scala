package pravda.dotnet.parsers

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import pravda.dotnet.data.Method
import pravda.dotnet.parsers.CIL.CilData
import pravda.dotnet.parsers.PE.Info.Pe

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

object FileParser {

  def parseFile(file: String): Either[String, (Pe, CilData, List[Method], Map[Long, Signatures.Signature])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    parsePe(fileBytes)
  }

  def parsePe(bytes: Array[Byte]): Either[String, (Pe, CilData, List[Method], Map[Long, Signatures.Signature])] = {
    val peV = PE.parseInfo(Bytes(bytes))

    for {
      pe <- peV
      cilData <- CIL.fromPeData(pe.peData)
      methods <- pe.methods.map(Method.parse(pe.peData, _)).sequence
      signatures <- Signatures.collectSignatures(cilData)
    } yield (pe, cilData, methods, signatures)
  }
}
