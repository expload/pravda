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

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data._
import pravda.vm.{Opcodes, asm}

case object ArgsLocalsTranslations extends OneToManyTranslator {

  override def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int] = {

    def loadArg(num: Int): Int =
      if (!ctx.local && num == 0) 0 else 1

    val offsetF: PartialFunction[CIL.Op, Int] = {
      case LdArg0      => loadArg(0)
      case LdArg1      => loadArg(1)
      case LdArg2      => loadArg(2)
      case LdArg3      => loadArg(3)
      case LdArg(num)  => loadArg(num)
      case LdArgS(num) => loadArg(num.toInt)
      case StLoc0      => -1
      case StLoc1      => -1
      case StLoc2      => -1
      case StLoc3      => -1
      case StLoc(num)  => -1
      case StLocS(num) => -1
      case LdLoc0      => 1
      case LdLoc1      => 1
      case LdLoc2      => 1
      case LdLoc3      => 1
      case LdLoc(num)  => 1
      case LdLocS(num) => 1
    }

    offsetF.lift(op).toRight(UnknownOpcode)
  }

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] = {

    def computeLocalOffset(num: Int, stackOffset: Int): Int =
      (ctx.localsCount - num - 1) + stackOffset + 1

    def computeArgOffset(num: Int, stackOffset: Int): Int =
      (ctx.argsCount - num - 1) + stackOffset + ctx.localsCount + 1 + 1
    // for local there's additional object arg
    // for not local there's name of the method

    def storeLocal(num: Int): Either[InternalError, List[asm.Operation]] =
      stackOffsetO
        .map { s =>
          Right(
            List(
              pushInt(computeLocalOffset(num, s)),
              asm.Operation(Opcodes.SWAPN),
              asm.Operation(Opcodes.POP)
            ))
        }
        .getOrElse(Left(InternalError("Stack offset is required for storing local variables")))

    def loadLocal(num: Int): Either[InternalError, List[asm.Operation]] =
      stackOffsetO
        .map { s =>
          Right(
            List(
              pushInt(computeLocalOffset(num, s)),
              asm.Operation(Opcodes.DUPN)
            ))
        }
        .getOrElse(Left(InternalError("Stack offset is required for loading local variables")))

    def loadArg(num: Int): Either[InternalError, List[asm.Operation]] =
      stackOffsetO
        .map { s =>
          if (ctx.local) {
            Right(
              List(
                pushInt(computeArgOffset(num, s)),
                asm.Operation(Opcodes.DUPN)
              )
            )
          } else {
            if (num == 0) {
              Right(List.empty) // skip this reference
            } else {
              Right(
                List(
                  pushInt(computeArgOffset(num - 1, s)),
                  asm.Operation(Opcodes.DUPN)
                )
              )
            }
          }
        }
        .getOrElse(Left(InternalError("Stack offset is required for arguments loading")))

    val translateF: PartialFunction[CIL.Op, Either[InternalError, List[asm.Operation]]] = {
      case LdArg0      => loadArg(0)
      case LdArg1      => loadArg(1)
      case LdArg2      => loadArg(2)
      case LdArg3      => loadArg(3)
      case LdArg(num)  => loadArg(num)
      case LdArgS(num) => loadArg(num.toInt)

      case StLoc0      => storeLocal(0)
      case StLoc1      => storeLocal(1)
      case StLoc2      => storeLocal(2)
      case StLoc3      => storeLocal(3)
      case StLoc(num)  => storeLocal(num)
      case StLocS(num) => storeLocal(num.toInt)

      case LdLoc0      => loadLocal(0)
      case LdLoc1      => loadLocal(1)
      case LdLoc2      => loadLocal(2)
      case LdLoc3      => loadLocal(3)
      case LdLoc(num)  => loadLocal(num)
      case LdLocS(num) => loadLocal(num.toInt)
    }

    translateF.lift(op).toRight(UnknownOpcode).joinRight
  }
}
