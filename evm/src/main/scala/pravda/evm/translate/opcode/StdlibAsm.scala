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

package pravda.evm.translate.opcode

import com.google.protobuf.ByteString
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes}

object StdlibAsm {

  /*
   * Stack:
   *         index
   *         Ref to byte array
   */
  lazy val readWord: List[Operation] = StdlibAsm.sliceArray ++ StdlibAsm.byteStringToBigint

  val readByte: List[Operation] =
    List(
      Operation(Opcodes.ARRAY_GET),
      pushType(Data.Type.BigInt),
      Operation(Opcodes.CAST),
    )

  val byteStringToBigint: List[Operation] =
    List(
      pushByte(0),
      pushType(Data.Type.Bytes),
      Operation(Opcodes.CAST),
      Operation(Opcodes.CONCAT),
      pushType(Data.Type.BigInt),
      Operation(Opcodes.CAST),
    )

  val bigintToByteString: List[Operation] =
    List(
      pushType(Data.Type.Bytes),
      Operation(Opcodes.CAST),
      Operation(Opcodes.DUP),
      Operation(Opcodes.LENGTH),
      pushInt(32),
      Operation(Opcodes.EQ),
      Operation.JumpI(Some("end_of_bigint_to_bytes_loop")),
      Operation.Label("bigint_to_bytes_loop"),
      pushByte(0),
      pushType(Data.Type.Bytes),
      Operation(Opcodes.CAST),
      Operation(Opcodes.CONCAT),
      Operation(Opcodes.DUP),
      Operation(Opcodes.LENGTH),
      pushInt(32),
      Operation(Opcodes.GT),
      Operation.JumpI(Some("bigint_to_bytes_loop")),
      Operation.Label("end_of_bigint_to_bytes_loop"),
    )

  /*
   * Stack:
   *         index
   *         word(BigInt)
   *         ref to byte array
   */
  val writeWord: List[Operation] = Operation(Opcodes.SWAP) :: bigintToByteString ++
    List(
      Operation(Opcodes.SWAP),
      pushInt(0),
      Operation.Label("write_words_loop"),
      Operation(Opcodes.DUP),
      pushInt(3),
      Operation(Opcodes.DUPN),
      Operation(Opcodes.SWAP),
      pushInt(5),
      Operation(Opcodes.DUPN),
      Operation(Opcodes.SWAP),
      Operation(Opcodes.ARRAY_GET),
      pushType(Data.Type.Int8),
      Operation(Opcodes.CAST),
      pushInt(6),
      Operation(Opcodes.DUPN),
      pushInt(3),
      Operation(Opcodes.SWAPN),
      Operation(Opcodes.ARRAY_MUT),
      Operation(Opcodes.SWAP),
      pushInt(1),
      Operation(Opcodes.ADD),
      Operation(Opcodes.SWAP),
      pushInt(1),
      Operation(Opcodes.ADD),
      Operation(Opcodes.DUP),
      pushInt(32),
      Operation(Opcodes.GT),
      Operation.JumpI(Some("write_words_loop")),
      Operation(Opcodes.POP),
      Operation(Opcodes.POP),
      Operation(Opcodes.POP),
      Operation(Opcodes.POP),
    )

  val sliceArray: List[Operation] =
    List(
      Operation(Opcodes.DUP),
      pushInt(32),
      Operation(Opcodes.ADD),
      Operation(Opcodes.SWAP),
      Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY)),
      Operation(Opcodes.SWAP),
      Operation.Label("array_to_bytes_loop"),
      pushInt(4),
      Operation(Opcodes.DUPN),
      pushInt(2),
      Operation(Opcodes.DUPN),
      Operation(Opcodes.ARRAY_GET),
      pushType(Data.Type.Bytes),
      Operation(Opcodes.CAST),
      pushInt(3),
      Operation(Opcodes.DUPN),
      Operation(Opcodes.CONCAT),
      pushInt(3),
      Operation(Opcodes.SWAPN),
      Operation(Opcodes.POP),
      pushInt(1),
      Operation(Opcodes.ADD),
      Operation(Opcodes.DUP),
      pushInt(4),
      Operation(Opcodes.DUPN),
      Operation(Opcodes.GT),
      Operation.JumpI(Some("array_to_bytes_loop")),
      Operation(Opcodes.POP),
      Operation(Opcodes.SWAP),
      Operation(Opcodes.POP),
      Operation(Opcodes.SWAP),
      Operation(Opcodes.POP),
      //    Operation(Opcodes.RET)
    )

}
