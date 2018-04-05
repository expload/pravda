package io.mytc.sood.asm


class BCGen {

  def gen(unit: Seq[Op]): Array[Byte] = {

    import scala.collection.mutable.ArrayBuffer

    import io.mytc.sood.vm
    import vm.{ Opcodes ⇒ OP }

    val code = new ArrayBuffer[Byte](unit.size)
    unit.foreach{
      case Op.Push(x) ⇒ {
                        code += OP.PUSHX
                       code ++= vm.int32ToWord(x)
                        }

      case Op.Stop    ⇒ code += OP.STOP
      case Op.Jump    ⇒ code += OP.JUMP
      case Op.JumpI   ⇒ code += OP.JUMPI

      case Op.Pop     ⇒ code += OP.POP
      case Op.Dup     ⇒ code += OP.DUP
      case Op.Swap    ⇒ code += OP.SWAP

      case Op.Call    ⇒ code += OP.CALL
      case Op.Ret     ⇒ code += OP.RET

      case Op.MPut    ⇒ code += OP.MPUT
      case Op.MGet    ⇒ code += OP.MGET

      case Op.I32Add  ⇒ code += OP.I32ADD
      case Op.I32Mul  ⇒ code += OP.I32MUL
      case Op.I32Div  ⇒ code += OP.I32DIV
      case Op.I32Mod  ⇒ code += OP.I32MOD

      case _          ⇒ throw new RuntimeException("Unsupported op code")
    }
    code.toArray
  }

}

object BCGen {
  def apply(): BCGen = new BCGen
}
