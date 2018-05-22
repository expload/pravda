package io.mytc.sood.cil

import io.mytc.sood.cil.CIL.OpCode
import io.mytc.sood.cil.PE.Info._
import io.mytc.sood.cil.Signatures.Info.LocalVarSig
import io.mytc.sood.cil.utils.{Validated, _}


final case class Method(opcodes: Seq[OpCode], maxStack: Int, localVarSig: LocalVarSig)

object Method {
  def parse(peData: PeData, header: MethodHeader): Validated[Method] = {
    header match {
      case FatMethodHeader(_, _, maxStack, localVarSigTok, codeBytes) =>
        if ((localVarSigTok >> 24) != 17) {
          validationError(s"Wrong local variables signature token: 0x${localVarSigTok.toHexString}")
        } else {
          val localVarSigIdx = (localVarSigTok & 0x00ffffff) - 1
          for {
            cilData <- CIL.fromPeData(peData)
            localVarSig <- cilData.tables.standAloneSigTable(localVarSigIdx.toInt).signature match {
              case l: LocalVarSig => validated(l)
              case e => validationError(s"Wrong signature: $e")
            }
            codeParser = CIL.code(cilData)
            code <- codeParser.parse(header.codeBytes).toValidated.joinRight
          } yield Method(code, maxStack, localVarSig)
        }
      case TinyMethodHeader(codeBytes) =>
        for {
          cilData <- CIL.fromPeData(peData)
          codeParser = CIL.code(cilData)
          code <- codeParser.parse(header.codeBytes).toValidated.joinRight
        } yield Method(code, 0, LocalVarSig(Seq.empty))
      case EmptyHeader =>
        validated(Method(Seq.empty, 0, LocalVarSig(Seq.empty)))
    }
  }
}
