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
  final case class Function(name: String, code: List[Operation])

  private def stdlibFunc(name: String, ops: List[Operation]): Function = {
    val fname = s"stdlib_$name"
    Function(fname,
             List(Operation.Label(fname)) ++
               ops ++ List(Operation(Opcodes.RET)))
  }

  val stdlibFuncs = List(
    stdlibFunc(
      "evm_sget",
      List(
        Operation(Opcodes.DUP),
        Operation(Opcodes.SEXIST),
        Operation.JumpI(Some("stdlib_evm_sget_non_zero")),
        Operation(Opcodes.POP),
        Operation.Push(Data.Primitive.Bytes(ByteString.copyFrom(Array.fill[Byte](32)(0)))),
        Operation(Opcodes.RET),
        Operation.Label("stdlib_evm_sget_non_zero"),
        Operation(Opcodes.SGET)
      )
    )
  )

}
