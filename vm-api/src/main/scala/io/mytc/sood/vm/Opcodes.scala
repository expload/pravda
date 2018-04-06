package io.mytc.sood.vm

object Opcodes {
  // Control
  final val STOP = int.STOP.toByte
  final val JUMP = int.JUMP.toByte
  final val JUMPI = int.JUMPI.toByte
  final val CALL = int.CALL.toByte
  final val RET = int.RET.toByte

  // Stack
  final val POP = int.POP.toByte
  final val PUSHX = int.PUSHX.toByte
  final val PUSH1 = int.PUSH1.toByte
  final val PUSH2 = int.PUSH2.toByte
  final val PUSH4 = int.PUSH4.toByte
  final val PUSH8 = int.PUSH8.toByte
  final val PUSH32 = int.PUSH32.toByte

  final val DUP = int.DUP.toByte
  final val SWAP = int.SWAP.toByte

  // Heap
  final val MPUT = int.MPUT.toByte
  final val MGET = int.MGET.toByte

  // Int32 operations
  final val I32ADD = int.I32ADD.toByte
  final val I32MUL = int.I32MUL.toByte
  final val I32DIV = int.I32DIV.toByte
  final val I32MOD = int.I32MUL.toByte

  object int {
    // Control
    final val STOP = 0x00
    final val JUMP = 0x01
    final val JUMPI = 0x02
    final val CALL = 0x04
    final val RET = 0x05

    // Stack
    final val POP = 0x10
    final val PUSHX = 0x11
    final val PUSH1 = 0x12
    final val PUSH2 = 0x13
    final val PUSH4 = 0x14
    final val PUSH8 = 0x15
    final val PUSH32 = 0x17

    final val DUP = 0x22
    final val SWAP = 0x33

    // Heap
    final val MPUT = 0x40
    final val MGET = 0x41

    // Int32 operations
    final val I32ADD = 0x60
    final val I32MUL = 0x61
    final val I32DIV = 0x62
    final val I32MOD = 0x63
  }
}
