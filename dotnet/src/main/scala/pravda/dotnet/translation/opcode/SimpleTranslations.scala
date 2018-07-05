package pravda.dotnet.translation.opcode

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data._
import pravda.vm.asm
import pravda.vm.{Data, Opcodes}

case object SimpleTranslations extends OpcodeTranslator {

  override def deltaOffset(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int] = {

    val offsetF: PartialFunction[CIL.Op, Int] = {
      case LdcI40     => 1
      case LdcI41     => 1
      case LdcI42     => 1
      case LdcI43     => 1
      case LdcI44     => 1
      case LdcI45     => 1
      case LdcI46     => 1
      case LdcI47     => 1
      case LdcI48     => 1
      case LdcI4M1    => 1
      case LdcI4(num) => 1
      case LdcI4S(v)  => 1
      case LdcR4(f)   => 1
      case LdcR8(d)   => 1
      case LdStr(s)   => 1

      case Add => -1
      case Mul => -1
      case Div => -1
      case Rem => -1
      case Sub => -1

      case Clt => -1
      case Cgt => -1
      case Ceq => -1
      case Not => 0

      case Nop => 0
      case Ret => 0
    }

    offsetF.lift(op).toRight(UnknownOpcode)
  }

  override def translate(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] = {

    val translateF: PartialFunction[CIL.Op, List[asm.Operation]] = {
      case LdcI40     => List(pushInt(0))
      case LdcI41     => List(pushInt(1))
      case LdcI42     => List(pushInt(2))
      case LdcI43     => List(pushInt(3))
      case LdcI44     => List(pushInt(4))
      case LdcI45     => List(pushInt(5))
      case LdcI46     => List(pushInt(6))
      case LdcI47     => List(pushInt(7))
      case LdcI48     => List(pushInt(8))
      case LdcI4M1    => List(pushInt(-1))
      case LdcI4(num) => List(pushInt(num))
      case LdcI4S(v)  => List(pushInt(v.toInt))
      case LdcR4(f)   => List(pushFloat(f.toDouble))
      case LdcR8(d)   => List(pushFloat(d))
      case LdStr(s)   => List(asm.Operation.Push(Data.Primitive.Utf8(s)))

      case Add => List(asm.Operation(Opcodes.ADD))
      case Mul => List(asm.Operation(Opcodes.MUL))
      case Div => List(asm.Operation(Opcodes.DIV))
      case Rem => List(asm.Operation(Opcodes.MOD))
      case Sub => List(pushInt(-1), asm.Operation(Opcodes.MUL), asm.Operation(Opcodes.ADD))

      case Clt =>
        asm.Operation(Opcodes.LT) :: cast(Data.Type.Int8)
      case Cgt =>
        asm.Operation(Opcodes.GT) :: cast(Data.Type.Int8)
      case Ceq =>
        asm.Operation(Opcodes.EQ) :: cast(Data.Type.Int8)
      case Not =>
        cast(Data.Type.Boolean) ++ (asm.Operation(Opcodes.NOT) :: cast(Data.Type.Int8))

      case Nop => List()
      case Ret => List()
    }

    translateF.lift(op).toRight(UnknownOpcode)
  }
}
