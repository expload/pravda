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

case object JumpsTranslation extends OneToManyTranslatorOnlyAsm {

  val labelStackOffset = 0
  val jumpStackOffset = 0
  val jumpIStackOffset = -1

  override def asmOpsOne(op: CIL.Op,
                         stackOffset: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] =
    op match {
      case Jump(label) => Right(List(asm.Operation.Jump(Some(label))))
      case JumpI(label) =>
        Right(List(pushInt(1), asm.Operation(Opcodes.EQ), asm.Operation.JumpI(Some(label))))
      case Label(label) => Right(List(asm.Operation.Label(label)))
      case _            => Left(UnknownOpcode)
    }
}
