package io.mytc.sood.asm

import java.nio.charset.StandardCharsets

class ByteCode {

  import io.mytc.sood.vm
  import vm.{Opcodes ⇒ VM}
  import scala.collection.mutable.ArrayBuffer

  def offsets(unit: Seq[Op]): Map[String, Int] = {
    val code = new ArrayBuffer[Byte](unit.size)
    val omap = unit
      .map {
        case Op.Push(d) ⇒ {
          val offset = code.size
          code += VM.PUSHX
          d match {
            case v: Datum.Integral ⇒ code ++= vm.int32ToWord(v.value)
            case v: Datum.Floating ⇒ code ++= vm.doubleToWord(v.value)
            case v: Datum.Rawbytes ⇒ code ++= vm.bytesToWord(v.value)
          }
          (Op.Push(d), offset)
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

        case Op.Jump(n) ⇒ {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(0)
          code += VM.JUMP
          (Op.Jump(n), offset)
        }

        case Op.JumpI(n) ⇒ {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(0)
          code += VM.JUMPI
          (Op.JumpI(n), offset)
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

        case Op.I32LT ⇒ {
          val offset = code.size
          code += VM.I32LT
          (Op.I32LT, offset)
        }

        case Op.I32GT ⇒ {
          val offset = code.size
          code += VM.I32GT
          (Op.I32GT, offset)
        }

        case Op.Eq ⇒ {
          val offset = code.size
          code += VM.EQ
          (Op.Eq, offset)
        }

        case Op.Concat ⇒ {
          val offset = code.size
          code += VM.CONCAT
          (Op.Concat, offset)
        }

        case Op.Not ⇒ {
          val offset = code.size
          code += VM.NOT
          (Op.Not, offset)
        }

        case Op.FAdd ⇒ {
          val offset = code.size
          code += VM.FADD
          (Op.FAdd, offset)
        }

        case Op.FMul ⇒ {
          val offset = code.size
          code += VM.FMUL
          (Op.FMul, offset)
        }

        case Op.FDiv ⇒ {
          val offset = code.size
          code += VM.FDIV
          (Op.FDiv, offset)
        }

        case Op.FMod ⇒ {
          val offset = code.size
          code += VM.FMOD
          (Op.FMod, offset)
        }

        case Op.Dupn ⇒ {
          val offset = code.size
          code += VM.DUPN
          (Op.Dupn, offset)
        }

        case Op.From ⇒ {
          val offset = code.size
          code += VM.FROM
          (Op.From, offset)
        }

        case Op.PCreate ⇒ {
          val offset = code.size
          code += VM.PCREATE
          (Op.PCreate, offset)
        }

        case Op.PUpdate ⇒ {
          val offset = code.size
          code += VM.PUPDATE
          (Op.PUpdate, offset)
        }

        case op @ Op.LCall(address, func, argsNum) =>
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.bytesToWord(address.getBytes(StandardCharsets.UTF_8))
          code ++= vm.bytesToWord(func.getBytes(StandardCharsets.UTF_8))
          code ++= vm.int32ToWord(argsNum)
          (op, offset)

        case Op.PCall =>
          val offset = code.size
          code += VM.PUSHX
          (Op.PCall, offset)

        case Op.SGet =>
          val offset = code.size
          code += VM.SGET
          (Op.SGet, offset)

        case Op.SPut =>
          val offset = code.size
          code += VM.SPUT
          (Op.SPut, offset)

        case Op.SExst =>
          val offset = code.size
          code += VM.SEXIST
          (Op.SExst, offset)

        case Op.Nop ⇒ {
          val offset = code.size
          (Op.Nop, offset)
        }

        case Op.Label(n) ⇒ {
          val offset = code.size
          (Op.Label(n), offset)
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
      case Op.Push(d) ⇒ {
        code += VM.PUSHX
        d match {
          case d: Datum.Integral ⇒ code ++= vm.int32ToWord(d.value)
          case d: Datum.Floating ⇒ code ++= vm.doubleToWord(d.value)
          case d: Datum.Rawbytes ⇒ code ++= vm.bytesToWord(d.value)
        }
      }

      case Op.Jump(n) ⇒ {
        code += VM.PUSHX
        code ++= vm.int32ToWord(offset(n))
        code += VM.JUMP
      }

      case Op.JumpI(n) ⇒ {
        code += VM.PUSHX
        code ++= vm.int32ToWord(offset(n))
        code += VM.JUMPI
      }

      case Op.Call(n) ⇒ {
        code += VM.PUSHX
        code ++= vm.int32ToWord(offset(n))
        code += VM.CALL
      }
      case Op.LCall(address, func, argsNum) =>
        code += VM.LCALL
        code ++= vm.bytesToWord(address.getBytes(StandardCharsets.UTF_8))
        code ++= vm.bytesToWord(func.getBytes(StandardCharsets.UTF_8))
        code ++= vm.int32ToWord(argsNum)

      case Op.PCall =>
        code += VM.PCALL

      case Op.SGet => code += VM.SGET
      case Op.SPut => code += VM.SPUT
      case Op.SExst => code += VM.SEXIST

      case Op.Label(n) ⇒ {}
      case Op.Stop     ⇒ code += VM.STOP
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
      case Op.I32LT    ⇒ code += VM.I32LT
      case Op.I32GT    ⇒ code += VM.I32GT
      case Op.Eq       ⇒ code += VM.EQ
      case Op.Not      ⇒ code += VM.NOT
      case Op.FAdd     ⇒ code += VM.FADD
      case Op.FMul     ⇒ code += VM.FMUL
      case Op.FDiv     ⇒ code += VM.FDIV
      case Op.FMod     ⇒ code += VM.FMOD
      case Op.Dupn     ⇒ code += VM.DUPN
      case Op.Concat   ⇒ code += VM.CONCAT
      case Op.From     ⇒ code += VM.FROM
      case Op.PCreate  ⇒ code += VM.PCREATE
      case Op.PUpdate  ⇒ code += VM.PUPDATE
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
      val pos = ubuf.position()
      val ins = ubuf.get()
      (ins & 0xFF) match {
        case VM.int.PUSHX   ⇒ obuf += ((pos, Op.Push(Datum.Rawbytes(wordToBytes(ubuf)))))
        case VM.int.CALL    ⇒ obuf += ((pos, Op.Call("")))
        case VM.int.STOP    ⇒ obuf += ((pos, Op.Stop))
        case VM.int.JUMP    ⇒ obuf += ((pos, Op.Jump("")))
        case VM.int.JUMPI   ⇒ obuf += ((pos, Op.JumpI("")))
        case VM.int.POP     ⇒ obuf += ((pos, Op.Pop))
        case VM.int.DUP     ⇒ obuf += ((pos, Op.Dup))
        case VM.int.SWAP    ⇒ obuf += ((pos, Op.Swap))
        case VM.int.RET     ⇒ obuf += ((pos, Op.Ret))
        case VM.int.MPUT    ⇒ obuf += ((pos, Op.MPut))
        case VM.int.MGET    ⇒ obuf += ((pos, Op.MGet))
        case VM.int.I32ADD  ⇒ obuf += ((pos, Op.I32Add))
        case VM.int.I32MUL  ⇒ obuf += ((pos, Op.I32Mul))
        case VM.int.I32DIV  ⇒ obuf += ((pos, Op.I32Div))
        case VM.int.I32MOD  ⇒ obuf += ((pos, Op.I32Mod))
        case VM.int.I32LT   ⇒ obuf += ((pos, Op.I32LT))
        case VM.int.I32GT   ⇒ obuf += ((pos, Op.I32GT))
        case VM.int.NOT     ⇒ obuf += ((pos, Op.Not))
        case VM.int.EQ      ⇒ obuf += ((pos, Op.Eq))
        case VM.int.FADD    ⇒ obuf += ((pos, Op.FAdd))
        case VM.int.FMUL    ⇒ obuf += ((pos, Op.FMul))
        case VM.int.FDIV    ⇒ obuf += ((pos, Op.FDiv))
        case VM.int.FMOD    ⇒ obuf += ((pos, Op.FMod))
        case VM.int.DUPN    ⇒ obuf += ((pos, Op.Dupn))
        case VM.int.CONCAT  ⇒ obuf += ((pos, Op.Concat))
        case VM.int.FROM    ⇒ obuf += ((pos, Op.From))
        case VM.int.PCREATE ⇒ obuf += ((pos, Op.PCreate))
        case VM.int.PUPDATE ⇒ obuf += ((pos, Op.PUpdate))
        case VM.int.PCALL   => obuf += ((pos, Op.PCall))
        case VM.int.LCALL =>
          obuf += ((pos,
                    Op.LCall(
                      new String(wordToBytes(ubuf), StandardCharsets.UTF_8),
                      new String(wordToBytes(ubuf), StandardCharsets.UTF_8),
                      wordToInt32(ubuf)
                    )))

        case VM.int.SGET => obuf += ((pos, Op.SGet))
        case VM.int.SPUT => obuf += ((pos, Op.SPut))
        case VM.int.SEXIST => obuf += ((pos, Op.SExst))
      }
    }

    obuf
  }

}

object ByteCode {
  def apply(): ByteCode = new ByteCode
}
