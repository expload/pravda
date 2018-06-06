package pravda.dotnet.data

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL.OpCode
import pravda.dotnet.parsers.PE.Info._
import pravda.dotnet.utils._

final case class Method(opcodes: Seq[OpCode], maxStack: Int, localVarSigIdx: Option[Long])

object Method {

  def parse(peData: PeData, header: MethodHeader): Either[String, Method] = {
    header match {
      case FatMethodHeader(_, _, maxStack, localVarSigTok, codeBytes) =>
        if ((localVarSigTok >> 24) != 17) {
          Left(s"Wrong local variables signature token: 0x${localVarSigTok.toHexString}")
        } else {
          val localVarSigIdx = (localVarSigTok & 0x00ffffff) - 1
          for {
            cilData <- CIL.fromPeData(peData)
            localVarSig = cilData.tables.standAloneSigTable(localVarSigIdx.toInt).signatureIdx
            codeParser = CIL.code(cilData)
            code <- codeParser.parse(header.codeBytes).toEither.joinRight
          } yield Method(code, maxStack, Some(localVarSig))
        }
      case TinyMethodHeader(codeBytes) =>
        for {
          cilData <- CIL.fromPeData(peData)
          codeParser = CIL.code(cilData)
          code <- codeParser.parse(header.codeBytes).toEither.joinRight
        } yield Method(code, 0, None)
      case EmptyHeader =>
        Right(Method(Seq.empty, 0, None))
    }
  }
}
