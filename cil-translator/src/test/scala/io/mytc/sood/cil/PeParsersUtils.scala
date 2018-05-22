package io.mytc.sood.cil

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import io.mytc.sood.cil.CIL.CilData
import io.mytc.sood.cil.PE.Info.Pe
import io.mytc.sood.cil.utils._

object PeParsersUtils {
  def parsePe(file: String): Validated[(Pe, CilData, Seq[Method])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    val peV = PE.parseInfo(Bytes(fileBytes))

    for {
      pe <- peV
      cilData <- CIL.fromPeData(pe.peData)
      methods <- pe.methods.map(Method.parse(pe.peData, _)).sequence
    } yield (pe, cilData, methods)
  }
}
