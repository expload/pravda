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

package pravda.dotnet.translation

import com.google.protobuf.ByteString
import pravda.common.vm.{Data, Opcodes}
import pravda.vm.asm

package object opcode {

  def push[T](value: T, toPrimitive: T => Data.Primitive): asm.Operation =
    asm.Operation.Push(toPrimitive(value))

  def pushInt(i: Int): asm.Operation =
    push(i, Data.Primitive.Int32)

  def pushLong(l: Long): asm.Operation =
    push(l, Data.Primitive.Int64)

  def pushFloat(d: Double): asm.Operation =
    push(d, Data.Primitive.Number)

  def pushString(s: String): asm.Operation =
    push(s, Data.Primitive.Utf8)

  def pushBytes(d: Array[Byte]): asm.Operation =
    push(d, (d: Array[Byte]) => Data.Primitive.Bytes(ByteString.copyFrom(d)))

  def pushBigInt(l: Long): asm.Operation =
    push(l, (l: Long) => Data.Primitive.BigInt(BigInt(l)))

  def pushType(tpe: Data.Type): asm.Operation =
    push(tpe, Data.Primitive.Int8)

  def cast(tpe: Data.Type): List[asm.Operation] =
    List(pushType(tpe), asm.Operation(Opcodes.CAST))

  def dupn(n: Int): List[asm.Operation] =
    List(pushInt(n), asm.Operation(Opcodes.DUPN))

  def swapn(n: Int): List[asm.Operation] =
    List(pushInt(n), asm.Operation(Opcodes.SWAPN))
}
