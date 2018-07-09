package pravda.dotnet.translation.opcode
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.PE
import pravda.dotnet.parsers.Signatures.FieldSig
import pravda.dotnet.parsers.Signatures.SigType.ValueTpe
import pravda.dotnet.translation.data.{MethodTranslationCtx, TranslationError, UnknownOpcode}
import pravda.vm.Data
import pravda.vm.asm.Operation

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object ArraysTranslation extends OpcodeTranslator {
  override def translate(ops: List[Op],
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, OpcodeTranslator.TranslationResult] = {
    ops.take(5) match {
      case List(
          OpcodeDetectors.IntLoad(arraySize),
          NewArr(TypeRefData(_, typeName, namespaceName)),
          Dup,
          LdToken(FieldData(_, fieldName, tokenSignIdx)),
          Call(MemberRefData(TypeRefData(_, "RuntimeHelpers", "System.Runtime.CompilerServices"), "InitializeArray", _))
          ) =>
        def bytesRva =
          for {
            rva <- ctx.cilData.tables.fieldRVATable.find(_.field.name == fieldName)
          } yield rva.rva

        def bytesSize =
          for {
            token <- ctx.signatures.get(tokenSignIdx)
            size <- token match {
              case FieldSig(ValueTpe(TypeDefData(_, fieldType, "", Ignored, Vector(), Vector())))
                  if fieldType.startsWith("__StaticArrayInitTypeSize=") =>
                Try { fieldType.drop("__StaticArrayInitTypeSize=".length).toInt }.toOption
            }
          } yield size

        def data(bytes: fastparse.byte.all.Bytes): Option[Data] = (namespaceName, typeName) match {
          case ("System", "Byte") => Some(Data.Array.Int8Array(bytes.toArray.toBuffer))
          case ("System", "Char") => Some(Data.Array.Int16Array(bytes.grouped(2).map(_.toShort()).toBuffer))
          case ("System", "Int32") => Some(Data.Array.Int32Array(bytes.grouped(4).map(_.toInt()).toBuffer))
          case ("System", "Uint32") => Some(Data.Array.Int32Array(bytes.grouped(4).map(_.toInt(signed = false)).toBuffer))
          case ("System", "Double") => Some(Data.Array.NumberArray(bytes.grouped(8).map(_.toByteBuffer.getDouble).toBuffer)
          case _ => None
        }

        (for {
          rva <- bytesRva
          size <- bytesSize
          bytes = PE.bytesFromRva(ctx.cilData.sections, rva, size)
          d <- data(bytes)
        } yield OpcodeTranslator.TranslationResult(ops.drop(5), List(Operation.Push(d)), 1)).toRight(UnknownOpcode)
    }
  }
}
