package pravda.vm

object Opcodes {

  // Control
  final val STOP = int.STOP.toByte
  final val JUMP = int.JUMP.toByte
  final val JUMPI = int.JUMPI.toByte
  final val CALL = int.CALL.toByte
  final val RET = int.RET.toByte
  final val PCALL = int.PCALL.toByte
  final val LCALL = int.LCALL.toByte
  final val PCREATE = int.PCREATE.toByte
  final val PUPDATE = int.PUPDATE.toByte

  // Stack
  final val POP = int.POP.toByte
  final val PUSHX = int.PUSHX.toByte
  final val DUP = int.DUP.toByte
  final val DUPN = int.DUPN.toByte
  final val SWAP = int.SWAP.toByte
  final val SWAPN = int.SWAPN.toByte

  // Heap
  final val NEW = int.NEW.toByte
  final val ARRAYGET = int.ARRAY_GET.toByte
  final val STRUCTGET = int.STRUCT_GET.toByte
  final val ARRAYMUT = int.ARRAY_MUT.toByte
  final val STRUCTMUT = int.STRUCT_MUT.toByte
  final val PRIMITEPUT = int.PRIMITE_PUT.toByte
  final val PRIMITIVEGET = int.PRIMITIVE_GET.toByte

  // Storage
  final val SPUT = int.SPUT.toByte
  final val SGET = int.SGET.toByte
  final val SDROP = int.SDROP.toByte
  final val SEXIST = int.SEXIST.toByte

  // Arithmetic operations
  final val ADD = int.ADD.toByte
  final val MUL = int.MUL.toByte
  final val DIV = int.DIV.toByte
  final val MOD = int.MOD.toByte
  final val LT = int.LT.toByte
  final val GT = int.GT.toByte

  // Boolean operations
  final val NOT = int.NOT.toByte
  final val AND = int.AND.toByte
  final val OR = int.OR.toByte
  final val XOR = int.XOR.toByte
  final val EQ = int.EQ.toByte

  // System operations
  final val FROM = int.FROM.toByte
  final val PADDR = int.PADDR.toByte
  final val TRANSFER = int.TRANSFER.toByte
  final val PTRANSFER = int.PTRANSFER.toByte

  object int {

    // Control
    final val STOP = 0x00
    final val JUMP = 0x01
    final val JUMPI = 0x02
    final val CALL = 0x04
    final val RET = 0x05
    final val PCALL = 0x06
    final val LCALL = 0x07
    final val PCREATE = 0x08
    final val PUPDATE = 0x09

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
    final val PRIMITE_PUT = 0x27
    final val PRIMITIVE_GET = 0x28
    
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

    // System operations
    final val FROM = 0xa0
    final val PADDR = 0xa2
    final val TRANSFER = 0xa3
    final val PTRANSFER = 0xa4
  }
}
