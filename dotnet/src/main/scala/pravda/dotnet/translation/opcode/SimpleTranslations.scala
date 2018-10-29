/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.dotnet.translation.opcode

import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._
import pravda.dotnet.translation.data._
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes}

case object SimpleTranslations extends OneToManyTranslatorOnlyAsm {

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[Operation]] = {

    val translateF: PartialFunction[CIL.Op, List[Operation]] = {
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
      case LdStr(s)   => List(Operation.Push(Data.Primitive.Utf8(s)))

      case ConvI1 => cast(Data.Type.Int8)
      case ConvU1 => cast(Data.Type.Int8)
      case ConvI2 => cast(Data.Type.Int16)
      case ConvU2 => cast(Data.Type.Int16)
      case ConvI4 => cast(Data.Type.Int32)
      case ConvU4 => cast(Data.Type.Int32)
      case ConvI8 => cast(Data.Type.BigInt)
      case ConvU8 => cast(Data.Type.BigInt)

      case Add => List(Operation(Opcodes.ADD))
      case Mul => List(Operation(Opcodes.MUL))
      case Div => List(Operation(Opcodes.SWAP), Operation(Opcodes.DIV))
      case Rem => List(Operation(Opcodes.SWAP), Operation(Opcodes.MOD))
      case Sub => List(pushInt(-1), Operation(Opcodes.MUL), Operation(Opcodes.ADD))

      case Clt   => Operation(Opcodes.SWAP) :: Operation(Opcodes.LT) :: cast(Data.Type.Int32)
      case CltUn => Operation(Opcodes.SWAP) :: Operation(Opcodes.LT) :: cast(Data.Type.Int32)
      case Cgt   => Operation(Opcodes.SWAP) :: Operation(Opcodes.GT) :: cast(Data.Type.Int32)
      case CgtUn => Operation(Opcodes.SWAP) :: Operation(Opcodes.GT) :: cast(Data.Type.Int32)
      case Ceq   => Operation(Opcodes.EQ) :: cast(Data.Type.Int32)
      case Not =>
        cast(Data.Type.Boolean) ++ (Operation(Opcodes.NOT) :: cast(Data.Type.Int32))

      case Dup => List(Operation(Opcodes.DUP))

      case Pop => List(Operation(Opcodes.POP))
      case Nop => List()
      case Ret => List(Operation.Jump(Some(s"${ctx.name}_lvc")))
    }

    translateF.lift(op).toRight(UnknownOpcode)
  }
}
