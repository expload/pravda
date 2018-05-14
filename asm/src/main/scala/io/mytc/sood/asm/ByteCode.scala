package io.mytc.sood.asm

import java.nio.charset.StandardCharsets


class ByteCode {

  import io.mytc.sood.vm
  import vm.{ Opcodes ⇒ VM }
  import scala.collection.mutable.ArrayBuffer

  def offsets(unit: Seq[Op]): Map[String, Int] = {
    val code = new ArrayBuffer[Byte](unit.size)
    val omap = unit.map{
      case Op.Push(d)  ⇒ {
                         val offset = code.size
                         code += VM.PUSHX
                         d match {
                           case v: Datum.Integral ⇒ code ++= vm.int32ToWord(v.value)
                           case v: Datum.Floating ⇒ code ++= vm.doubleToWord(v.value)
                           case v: Datum.Rawbytes ⇒ code ++= vm.bytesToWord(v.value)
                         }
                         (Op.Push(d), offset)
                         }

      case Op.Call(n)  ⇒ {
                         val offset = code.size
                         code += VM.PUSHX
                         code ++= vm.int32ToWord(0)
                         code += VM.CALL
                         (Op.Call(n), offset)
                         }

      case Op.Stop     ⇒ {
                         val offset = code.size
                         code += VM.STOP
                         (Op.Stop, offset)
                         }

      case Op.Jump     ⇒ {
                         val offset = code.size
                         code += VM.JUMP
                         (Op.Jump, offset)
                         }

      case Op.JumpI    ⇒ {
                         val offset = code.size
                         code += VM.JUMPI
                         (Op.JumpI, offset)
                         }

      case Op.Pop      ⇒ {
                         val offset = code.size
                         code += VM.POP
                         (Op.Pop, offset)
                         }

      case Op.Dup      ⇒ {
                         val offset = code.size
                         code += VM.DUP
                         (Op.Dup, offset)
                         }

      case Op.Swap     ⇒ {
                         val offset = code.size
                         code += VM.SWAP
                         (Op.Swap, offset)
                         }

      case Op.Ret      ⇒ {
                         val offset = code.size
                         code += VM.RET
                         (Op.Ret, offset)
                         }

      case Op.MPut     ⇒ {
                         val offset = code.size
                         code += VM.MPUT
                         (Op.MPut, offset)
                         }

      case Op.MGet     ⇒ {
                         val offset = code.size
                         code += VM.MGET
                         (Op.MGet, offset)
                         }

      case Op.I32Add   ⇒ {
                         val offset = code.size
                         code += VM.I32ADD
                         (Op.I32Add, offset)
                         }

      case Op.I32Mul   ⇒ {
                         val offset = code.size
                         code += VM.I32MUL
                         (Op.I32Mul, offset)
                         }

      case Op.I32Div   ⇒ {
                         val offset = code.size
                         code += VM.I32DIV
                         (Op.I32Div, offset)
                         }

      case Op.I32Mod   ⇒ {
                         val offset = code.size
                         code += VM.I32MOD
                         (Op.I32Mod, offset)
                         }

      case Op.FAdd   ⇒ {
                         val offset = code.size
                         code += VM.FADD
                         (Op.FAdd, offset)
                         }

      case Op.FMul   ⇒ {
                         val offset = code.size
                         code += VM.FMUL
                         (Op.FMul, offset)
                         }

      case Op.FDiv   ⇒ {
                         val offset = code.size
                         code += VM.FDIV
                         (Op.FDiv, offset)
                         }

      case Op.FMod   ⇒ {
                         val offset = code.size
                         code += VM.FMOD
                         (Op.FMod, offset)
                         }

      case Op.Label(n) ⇒ {
                         val offset = code.size
                         (Op.Label(n), offset)
                         }

      case Op.Nop      ⇒ {
                         val offset = code.size
                         (Op.Nop, offset)
                         }
      case op @ Op.LCall(adress, func, argsNum) =>
        val offset = code.size
        code += VM.PUSHX
        code ++= vm.bytesToWord(adress.getBytes(StandardCharsets.UTF_8))
        code ++= vm.bytesToWord(func.getBytes(StandardCharsets.UTF_8))
        code ++= vm.int32ToWord(argsNum)
        (op, offset)
    }.collect{ case (Op.Label(n), v) ⇒ (n, v) }.toMap
    omap
  }

  def gen(unit: Seq[Op]): Array[Byte] = {

    val offset = offsets(unit)
    val code = new ArrayBuffer[Byte](unit.size)

    unit.foreach{
      case Op.Push(d)  ⇒ {
                           code += VM.PUSHX
                           d match {
                             case d: Datum.Integral ⇒ code ++= vm.int32ToWord(d.value)
                             case d: Datum.Floating ⇒ code ++= vm.doubleToWord(d.value)
                             case d: Datum.Rawbytes ⇒ code ++= vm.bytesToWord(d.value)
                           }
                         }

      case Op.Call(n)  ⇒ {
                         code += VM.PUSHX
                         code ++= vm.int32ToWord(offset(n))
                         code += VM.CALL
                         }
      case Op.LCall(adress, func, argsNum) =>
        code += VM.PUSHX
        code ++= vm.bytesToWord(adress.getBytes(StandardCharsets.UTF_8))
        code ++= vm.bytesToWord(func.getBytes(StandardCharsets.UTF_8))
        code ++= vm.int32ToWord(argsNum)

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
      case Op.FAdd     ⇒ code += VM.FADD
      case Op.FMul     ⇒ code += VM.FMUL
      case Op.FDiv     ⇒ code += VM.FDIV
      case Op.FMod     ⇒ code += VM.FMOD
      case Op.Nop      ⇒ code += VM.I32MOD
    }

    code.toArray
  }

  def ungen(unit: Array[Byte]): Seq[(Int, Op)] = {

    import java.nio.ByteBuffer
    import vm._

    val ubuf = ByteBuffer.wrap(unit)
    val obuf = new ArrayBuffer[(Int, Op)]()

    while (ubuf.remaining > 0) {
      val pos = ubuf.position
      ubuf.get() & 0xFF match {
        case VM.PUSHX    ⇒ obuf += ((pos, Op.Push(Datum.Rawbytes(wordToBytes(ubuf)))))
        case VM.CALL     ⇒ obuf += ((pos, Op.Call("")))
        case VM.STOP     ⇒ obuf += ((pos, Op.Stop    ))
        case VM.JUMP     ⇒ obuf += ((pos, Op.Jump    ))
        case VM.JUMPI    ⇒ obuf += ((pos, Op.JumpI   ))
        case VM.POP      ⇒ obuf += ((pos, Op.Pop     ))
        case VM.DUP      ⇒ obuf += ((pos, Op.Dup     ))
        case VM.SWAP     ⇒ obuf += ((pos, Op.Swap    ))
        case VM.RET      ⇒ obuf += ((pos, Op.Ret     ))
        case VM.MPUT     ⇒ obuf += ((pos, Op.MPut    ))
        case VM.MGET     ⇒ obuf += ((pos, Op.MGet    ))
        case VM.I32ADD   ⇒ obuf += ((pos, Op.I32Add  ))
        case VM.I32MUL   ⇒ obuf += ((pos, Op.I32Mul  ))
        case VM.I32DIV   ⇒ obuf += ((pos, Op.I32Div  ))
        case VM.I32MOD   ⇒ obuf += ((pos, Op.I32Mod  ))
        case VM.FADD     ⇒ obuf += ((pos, Op.FAdd    ))
        case VM.FMUL     ⇒ obuf += ((pos, Op.FMul    ))
        case VM.FDIV     ⇒ obuf += ((pos, Op.FDiv    ))
        case VM.FMOD     ⇒ obuf += ((pos, Op.FMod    ))
        case VM.LCALL    =>
          obuf += ((pos,
                    Op.LCall(
                      new String(wordToBytes(ubuf), StandardCharsets.UTF_8),
                      new String(wordToBytes(ubuf), StandardCharsets.UTF_8),
                      wordToInt32(ubuf)
                    )))
      }
    }

    obuf.toSeq
  }

}

object ByteCode {
  def apply(): ByteCode = new ByteCode
}
