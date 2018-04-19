package io.mytc.sood.asm

class ByteCode {

  import io.mytc.sood.vm
  import vm.{Opcodes ⇒ VM}
  import scala.collection.mutable.ArrayBuffer

  def offsets(unit: Seq[Op]): Map[String, Int] = {
    val code = new ArrayBuffer[Byte](unit.size)
    val omap = unit
      .map {
        case Op.Push(x) ⇒ {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(x)
          (Op.Push(x), offset)
        }

        case Op.Call(n) ⇒ {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(0)
          code += VM.CALL
          (Op.Call(n), offset)
        }

        case Op.Stop ⇒ {
          val offset = code.size
          code += VM.STOP
          (Op.Stop, offset)
        }

        case Op.Jump ⇒ {
          val offset = code.size
          code += VM.JUMP
          (Op.Jump, offset)
        }

        case Op.JumpI ⇒ {
          val offset = code.size
          code += VM.JUMPI
          (Op.JumpI, offset)
        }

        case Op.Pop ⇒ {
          val offset = code.size
          code += VM.POP
          (Op.Pop, offset)
        }

        case Op.Dup ⇒ {
          val offset = code.size
          code += VM.DUP
          (Op.Dup, offset)
        }

        case Op.Swap ⇒ {
          val offset = code.size
          code += VM.SWAP
          (Op.Swap, offset)
        }

        case Op.Ret ⇒ {
          val offset = code.size
          code += VM.RET
          (Op.Ret, offset)
        }

        case Op.MPut ⇒ {
          val offset = code.size
          code += VM.MPUT
          (Op.MPut, offset)
        }

        case Op.MGet ⇒ {
          val offset = code.size
          code += VM.MGET
          (Op.MGet, offset)
        }

        case Op.I32Add ⇒ {
          val offset = code.size
          code += VM.I32ADD
          (Op.I32Add, offset)
        }

        case Op.I32Mul ⇒ {
          val offset = code.size
          code += VM.I32MUL
          (Op.I32Mul, offset)
        }

        case Op.I32Div ⇒ {
          val offset = code.size
          code += VM.I32DIV
          (Op.I32Div, offset)
        }

        case Op.I32Mod ⇒ {
          val offset = code.size
          code += VM.I32MOD
          (Op.I32Mod, offset)
        }

        case Op.Label(n) ⇒ {
          val offset = code.size
          (Op.Label(n), offset)
        }

        case Op.Nop ⇒ {
          val offset = code.size
          (Op.Nop, offset)
        }
      }
      .collect { case (Op.Label(n), v) ⇒ (n, v) }
      .toMap
    omap
  }

  def gen(unit: Seq[Op]): Array[Byte] = {

    val offset = offsets(unit)
    val code = new ArrayBuffer[Byte](unit.size)

    unit.foreach {
      case Op.Push(x) ⇒ {
        code += VM.PUSHX
        code ++= vm.int32ToWord(x)
      }

      case Op.Call(n) ⇒ {
        code += VM.PUSHX
        code ++= vm.int32ToWord(offset(n))
        code += VM.CALL
      }

      case Op.Label(n) ⇒ {}
      case Op.Stop     ⇒ code += VM.STOP
      case Op.Jump     ⇒ code += VM.JUMP
      case Op.JumpI    ⇒ code += VM.JUMPI
      case Op.Pop      ⇒ code += VM.POP
      case Op.Dup      ⇒ code += VM.DUP
      case Op.Swap     ⇒ code += VM.SWAP
      case Op.Ret      ⇒ code += VM.RET
      case Op.MPut     ⇒ code += VM.MPUT
      case Op.MGet     ⇒ code += VM.MGET
      case Op.I32Add   ⇒ code += VM.I32ADD
      case Op.I32Mul   ⇒ code += VM.I32MUL
      case Op.I32Div   ⇒ code += VM.I32DIV
      case Op.I32Mod   ⇒ code += VM.I32MOD
      case Op.Nop      ⇒ code += VM.I32MOD
    }

    code.toArray
  }

  def ungen(unit: Array[Byte]): Seq[(Int, Op)] = {

    import java.nio.ByteBuffer
    import vm.wordToBytes

    val ubuf = ByteBuffer.wrap(unit)
    val obuf = new ArrayBuffer[(Int, Op)]()

    while (ubuf.remaining > 0) {
      val pos = ubuf.position
      ubuf.get() & 0xFF match {
        case VM.PUSHX  ⇒ obuf += ((pos, Op.Push(wordToBytes(ubuf).sum.toInt)))
        case VM.CALL   ⇒ obuf += ((pos, Op.Call("")))
        case VM.STOP   ⇒ obuf += ((pos, Op.Stop))
        case VM.JUMP   ⇒ obuf += ((pos, Op.Jump))
        case VM.JUMPI  ⇒ obuf += ((pos, Op.JumpI))
        case VM.POP    ⇒ obuf += ((pos, Op.Pop))
        case VM.DUP    ⇒ obuf += ((pos, Op.Dup))
        case VM.SWAP   ⇒ obuf += ((pos, Op.Swap))
        case VM.RET    ⇒ obuf += ((pos, Op.Ret))
        case VM.MPUT   ⇒ obuf += ((pos, Op.MPut))
        case VM.MGET   ⇒ obuf += ((pos, Op.MGet))
        case VM.I32ADD ⇒ obuf += ((pos, Op.I32Add))
        case VM.I32MUL ⇒ obuf += ((pos, Op.I32Mul))
        case VM.I32DIV ⇒ obuf += ((pos, Op.I32Div))
        case VM.I32MOD ⇒ obuf += ((pos, Op.I32Mod))
      }
    }

    obuf.toSeq
  }

}

object ByteCode {
  def apply(): ByteCode = new ByteCode
}
