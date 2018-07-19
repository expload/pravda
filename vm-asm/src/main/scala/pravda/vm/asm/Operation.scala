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

package pravda.vm.asm

import pravda.vm.Data.Primitive
import pravda.vm.{Data, Opcodes}

sealed trait Operation

object Operation {

  import Opcodes._

  // Virtual operations (which aren't included to bytecode)
  case object Nop                         extends Operation
  final case class Comment(value: String) extends Operation
  final case class Label(name: String)    extends Operation

  // TODO add meta to parser
  final case class Meta(meta: pravda.vm.Meta)        extends Operation
  final case class Push(d: Data)                     extends Operation // why Data, not Data.Primitive?
  final case class New(d: Data)                      extends Operation
  final case class Jump(name: Option[String])        extends Operation
  final case class JumpI(name: Option[String])       extends Operation
  final case class Call(name: Option[String])        extends Operation
  final case class StructMut(key: Option[Primitive]) extends Operation
  final case class StructGet(key: Option[Primitive]) extends Operation

  final case class Orphan(opcode: Int) extends Operation

  val Orphans: Seq[Operation.Orphan] = Seq(
    Orphan(STOP),
    Orphan(RET),
    Orphan(PCALL),
    Orphan(LCALL),
    Orphan(SCALL),
    Orphan(POP),
    Orphan(DUPN),
    Orphan(DUP),
    Orphan(SWAPN),
    Orphan(SWAP),
    Orphan(ARRAY_GET),
    Orphan(ARRAY_MUT),
    Orphan(PRIMITIVE_PUT),
    Orphan(PRIMITIVE_GET),
    Orphan(SPUT),
    Orphan(SGET),
    Orphan(SDROP),
    Orphan(SEXIST),
    Orphan(ADD),
    Orphan(MUL),
    Orphan(DIV),
    Orphan(MOD),
    Orphan(LT),
    Orphan(GT),
    Orphan(NOT),
    Orphan(AND),
    Orphan(OR),
    Orphan(XOR),
    Orphan(EQ),
    Orphan(CAST),
    Orphan(CONCAT),
    Orphan(SLICE),
    Orphan(FROM),
    Orphan(PADDR),
    Orphan(PCREATE),
    Orphan(PUPDATE),
    Orphan(TRANSFER),
    Orphan(PTRANSFER),
    Orphan(NEW_ARRAY),
    Orphan(LENGTH)
  )

  val mnemonicByOpcode: Map[Int, String] = Map(
    PUSHX -> "push",
    NEW -> "new",
    JUMP -> "jump",
    JUMPI -> "jumpi",
    CALL -> "call",
    STRUCT_MUT -> "struct_mut",
    STRUCT_GET -> "struct_get",
    STOP -> "stop",
    RET -> "ret",
    PCALL -> "pcall",
    LCALL -> "lcall",
    SCALL -> "scall",
    POP -> "pop",
    DUPN -> "dupn",
    DUP -> "dup",
    SWAPN -> "swapn",
    SWAP -> "swap",
    ARRAY_GET -> "array_get",
    ARRAY_MUT -> "array_mut",
    PRIMITIVE_PUT -> "primitive_put",
    PRIMITIVE_GET -> "primitive_get",
    SPUT -> "sput",
    SGET -> "sget",
    SDROP -> "sdrop",
    SEXIST -> "sexist",
    ADD -> "add",
    MUL -> "mul",
    DIV -> "div",
    MOD -> "mod",
    LT -> "lt",
    GT -> "gt",
    NOT -> "not",
    AND -> "and",
    OR -> "or",
    XOR -> "xor",
    EQ -> "eq",
    CAST -> "cast",
    CONCAT -> "concat",
    SLICE -> "slice",
    FROM -> "from",
    PADDR -> "paddr",
    PCREATE -> "pcreate",
    PUPDATE -> "pupdate",
    TRANSFER -> "transfer",
    PTRANSFER -> "ptransfer",
    META -> "meta",
    NEW_ARRAY -> "new_array",
    LENGTH -> "length"
  )

  /**
    * Orphan operation by opcode.
    */
  val operationByOpcode: Map[Int, Operation] = Orphans
    .map(o => o.opcode -> o)
    .toMap

  /**
    * Alias to [[operationByOpcode]]
    */
  def apply(opcode: Int): Operation =
    operationByOpcode(opcode)
}
