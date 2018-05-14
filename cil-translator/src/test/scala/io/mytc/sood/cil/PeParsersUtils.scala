package io.mytc.sood.cil

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import io.mytc.sood.cil.CIL.{CilData, OpCode}
import io.mytc.sood.cil.PE.Info.Pe
import io.mytc.sood.cil.utils._

object PeParsersUtils {
  def parsePe(file: String): Validated[(Pe, CilData, Seq[Seq[OpCode]])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    val peV = PE.parseInfo(Bytes(fileBytes))

    for {
      pe <- peV
      cilData <- CIL.fromPeData(pe.peData)
      codeParser = CIL.code(cilData)
      ops <- pe.methods.map(m => codeParser.parse(m.codeBytes).toValidated.joinRight).sequence
    } yield (pe, cilData, ops)
  }
}
