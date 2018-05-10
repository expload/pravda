package io.mytc.sood.vm

object Opcodes {
  // Control
  final val STOP = int.STOP.toByte
  final val JUMP = int.JUMP.toByte
  final val JUMPI = int.JUMPI.toByte
  final val CALL = int.CALL.toByte
  final val RET = int.RET.toByte

  final val PCALL = int.PCALL.toByte
  final val LCALL = int.LCALL.toByte

  // Stack
  final val POP = int.POP.toByte
  final val PUSHX = int.PUSHX.toByte

  final val DUP = int.DUP.toByte
  final val SWAP = int.SWAP.toByte

  // Heap
  final val MPUT = int.MPUT.toByte
  final val MGET = int.MGET.toByte

  // Storage
  final val SPUT = int.SPUT.toByte
  final val SGET = int.SGET.toByte
  final val SDROP = int.SDROP.toByte

  // Int32 operations
  final val I32ADD = int.I32ADD.toByte
  final val I32MUL = int.I32MUL.toByte
  final val I32DIV = int.I32DIV.toByte
  final val I32MOD = int.I32MUL.toByte

  // Strict float operations
  final val FADD = int.FADD.toByte
  final val FMUL = int.FMUL.toByte
  final val FDIV = int.FDIV.toByte
  final val FMOD = int.FMUL.toByte

  // Boolean operations
  final val NOT = int.NOT.toByte
  final val AND = int.AND.toByte
  final val OR = int.OR.toByte
  final val XOR = int.XOR.toByte
  final val EQ = int.EQ.toByte
  final val I32LT = int.I32LT.toByte
  final val I32GT = int.I32GT.toByte

  final val FTBL = int.FTBL.toByte

  object int {
    // Control
    final val STOP = 0x00
    final val JUMP = 0x01
    final val JUMPI = 0x02
    final val CALL = 0x04
    final val RET = 0x05

    final val PCALL = 0x06
    final val LCALL = 0x07

    // Stack
    final val POP = 0x10
    final val PUSHX = 0x11

    final val DUP = 0x22
    final val SWAP = 0x33

    // Heap
    final val MPUT = 0x43
    final val MGET = 0x44

    // Storage
    final val SPUT = 0x50
    final val SGET = 0x51
    final val SDROP = 0x52

    // Int32 operations
    final val I32ADD = 0x60
    final val I32MUL = 0x61
    final val I32DIV = 0x62
    final val I32MOD = 0x63
    final val I32LT = 0x67
    final val I32GT = 0x68

    // Double precision numbers operations
    final val FADD = 0x70
    final val FMUL = 0x71
    final val FDIV = 0x72
    final val FMOD = 0x73

    // Boolean operations
    final val NOT = 0x80
    final val AND = 0x81
    final val OR = 0x82
    final val XOR = 0x83
    final val EQ = 0x84

    final val FTBL = 0xfa

  }
}
