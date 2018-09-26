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

package pravda.vm

object Opcodes {

  // Control
  final val STOP = 0x00
  final val JUMP = 0x01
  final val JUMPI = 0x02
  final val CALL = 0x04
  final val RET = 0x05
  final val PCALL = 0x06
  final val LCALL = 0x07
  final val SCALL = 0x08

  // Stack
  final val POP = 0x10
  final val PUSHX = 0x11
  final val DUP = 0x12
  final val DUPN = 0x13
  final val SWAP = 0x14
  final val SWAPN = 0x15

  // Heap
  final val NEW = 0x20
  final val ARRAY_GET = 0x21
  final val STRUCT_GET = 0x22
  final val STRUCT_GET_STATIC = 0x23
  final val ARRAY_MUT = 0x24
  final val STRUCT_MUT = 0x25
  final val STRUCT_MUT_STATIC = 0x26
  final val PRIMITIVE_PUT = 0x27
  final val PRIMITIVE_GET = 0x28
  final val NEW_ARRAY = 0x29
  final val LENGTH = 0x30

  // Storage
  final val SPUT = 0x50
  final val SGET = 0x51
  final val SDROP = 0x52
  final val SEXIST = 0x53

  // Arithmetic operations
  final val ADD = 0x60
  final val MUL = 0x61
  final val DIV = 0x62
  final val MOD = 0x63
  final val LT = 0x67
  final val GT = 0x68

  // Boolean operations
  final val NOT = 0x80
  final val AND = 0x81
  final val OR = 0x82
  final val XOR = 0x83
  final val EQ = 0x84

  // Data operations
  final val CAST = 0x90
  final val CONCAT = 0x91
  final val SLICE = 0x92

  // System operations
  final val FROM = 0xa0
  final val META = 0xa1
  final val PADDR = 0xa2
  final val PCREATE = 0xa5
  final val PUPDATE = 0xa6
  final val OWNER = 0xa7
  final val SEAL = 0xa8
  final val THROW = 0xa9
  final val EVENT = 0xaa

  // Native coins
  final val TRANSFER = 0xc0
  final val PTRANSFER = 0xc1
  final val BALANCE = 0xc2
}
