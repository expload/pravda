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

package pravda.evm.translate

import com.google.protobuf.ByteString
import pravda.vm.{Data, Opcodes, asm}

//TODO merge with pravda.dotnet.translation.opcode.opcode
package object opcode {

  def getNameByAddress(n: Int): String = s"_lbl_$n"

  def push[T](value: T, toPrimitive: T => Data.Primitive): asm.Operation =
    asm.Operation.Push(toPrimitive(value))

  val sub
    : List[asm.Operation] = asm.Operation(Opcodes.SWAP) :: pushBigInt(BigInt(-1)) :: asm.Operation(Opcodes.MUL) :: asm
    .Operation(Opcodes.ADD) :: Nil

  val callExp: List[asm.Operation] = pushInt(3) :: asm.Operation(Opcodes.SCALL) :: Nil

  def pushBigInt(value: scala.BigInt): asm.Operation =
    push(value, Data.Primitive.BigInt)

  def pushInt(i: Int): asm.Operation =
    push(i, Data.Primitive.Int32)

  def pushFloat(d: Double): asm.Operation =
    push(d, Data.Primitive.Number)

  def pushString(s: String): asm.Operation =
    push(s, Data.Primitive.Utf8)

  def pushBytes(d: Array[Byte]): asm.Operation =
    push(d, (d: Array[Byte]) => Data.Primitive.Bytes(ByteString.copyFrom(d)))

  def pushType(tpe: Data.Type): asm.Operation =
    push(tpe, Data.Primitive.Int8)

  def cast(tpe: Data.Type): List[asm.Operation] =
    List(pushType(tpe), asm.Operation(Opcodes.CAST))

  def dupn(n: Int): List[asm.Operation] =
    List(pushInt(n), asm.Operation(Opcodes.DUPN))

  def swapn(n: Int): List[asm.Operation] =
    List(pushInt(n), asm.Operation(Opcodes.SWAPN))
}
