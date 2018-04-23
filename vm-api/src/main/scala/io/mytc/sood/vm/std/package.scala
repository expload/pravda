package io.mytc.sood.vm

package object std {

  case class StdFunc(lcode: Byte, fcode: Byte) {
    val code = Array(lcode, fcode)
    val call = Array(Opcodes.PUSHX) ++ bytesToWord(code) :+ Opcodes.DCALL
  }

  sealed trait Lib {
    protected val icode: Int
    protected def code(code: Int) = StdFunc(CODE, code.toByte)

    val CODE: Byte = icode.toByte
  }

  object Math extends Lib {
    override protected val icode: Int = 0

    val SUM = code(0)
    val SUMN = code(1)

  }

  object Hash extends Lib {
    override protected val icode: Int = 1

    val SHA3 = code(0)

  }

}
