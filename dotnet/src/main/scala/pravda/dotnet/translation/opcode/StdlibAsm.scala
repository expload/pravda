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
import com.google.protobuf.ByteString
import pravda.dotnet.translation.data.{MethodTranslation, OpCodeTranslation}
import pravda.common.vm.{Data, Opcodes}
import pravda.vm.asm.Operation

/**
  * Helpful functions used in different Translators.
  *
  * They all are added to the resulted Pravda code,
  * but then some of them can be removed by dead code elimination.
  */
object StdlibAsm {

  private def stdlibFunc(name: String, ops: List[Operation]): MethodTranslation =
    MethodTranslation(
      "stdlib",
      name,
      forceAdd = false,
      List(
        OpCodeTranslation(
          List.empty,
          ops
        )
      )
    )

  // function needed to implement `Mapping.GetOrDefault`
  val stdlibFuncs: List[MethodTranslation] = List(
    stdlibFunc(
      "storage_get_default",
      dupn(2) ++
        cast(Data.Type.Bytes) ++
        dupn(4) ++
        List(
          Operation.Orphan(Opcodes.CONCAT),
          Operation.Orphan(Opcodes.SEXIST),
          Operation.JumpI(Some("get_default_if")),
          Operation.Orphan(Opcodes.SWAP),
          Operation.Orphan(Opcodes.POP),
          Operation.Orphan(Opcodes.SWAP),
          Operation.Orphan(Opcodes.POP),
          Operation.Orphan(Opcodes.RET),
          Operation.Label("get_default_if"),
          Operation.Orphan(Opcodes.POP)
        ) ++
        cast(Data.Type.Bytes) ++
        List(
          Operation.Orphan(Opcodes.SWAP),
          Operation.Orphan(Opcodes.CONCAT),
          Operation.Orphan(Opcodes.SGET),
          Operation.Orphan(Opcodes.RET)
        )
    ),
    // convert array of bytes to immutable Bytes
    stdlibFunc(
      "array_to_bytes",
      List(
        Operation(Opcodes.DUP),
        Operation(Opcodes.LENGTH),
        Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY)),
        pushInt(0),
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
        Operation(Opcodes.RET)
      )
    ),
    // concat array of strings to one big string
    stdlibFunc(
      "concat_all_string",
      List(
        Operation(Opcodes.DUP),
        Operation(Opcodes.LENGTH),
        Operation.Push(Data.Primitive.Utf8("")),
        pushInt(0),
        Operation.Label("concat_all_string_loop"),
        pushInt(4),
        Operation(Opcodes.DUPN),
        pushInt(2),
        Operation(Opcodes.DUPN),
        Operation(Opcodes.ARRAY_GET),
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
        Operation.JumpI(Some("concat_all_string_loop")),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.RET)
      )
    )
  )
}
