package pravda.vm.asm

import com.google.protobuf.ByteString
import java.nio.charset.StandardCharsets

class ByteCode {

  import pravda.vm
  import vm.{Opcodes => VM}
  import scala.collection.mutable.ArrayBuffer

  def offsets(unit: Seq[Op]): Map[String, Int] = {
    val code = new ArrayBuffer[Byte](unit.size)
    val omap = unit
      .map {
        case Op.Push(d) => {
          val offset = code.size
          code += VM.PUSHX
          d match {
            case v: Datum.Integral => code ++= vm.int32ToWord(v.value)
            case v: Datum.Floating => code ++= vm.doubleToWord(v.value)
            case v: Datum.Rawbytes => code ++= vm.bytesToWord(v.value)
          }
          (Op.Push(d), offset)
        }

        case Op.Call(n) => {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(0)
          code += VM.CALL
          (Op.Call(n), offset)
        }

        case Op.Stop => {
          val offset = code.size
          code += VM.STOP
          (Op.Stop, offset)
        }

        case Op.Jump(n) => {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(0)
          code += VM.JUMP
          (Op.Jump(n), offset)
        }

        case Op.JumpI(n) => {
          val offset = code.size
          code += VM.PUSHX
          code ++= vm.int32ToWord(0)
          code += VM.JUMPI
          (Op.JumpI(n), offset)
        }

        case Op.Pop => {
          val offset = code.size
          code += VM.POP
          (Op.Pop, offset)
        }

        case Op.Dup => {
          val offset = code.size
          code += VM.DUP
          (Op.Dup, offset)
        }

        case Op.Swap => {
          val offset = code.size
          code += VM.SWAP
          (Op.Swap, offset)
        }

        case Op.Ret => {
          val offset = code.size
          code += VM.RET
          (Op.Ret, offset)
        }

        case Op.SwapN => {
          val offset = code.size
          code += VM.SWAPN
          (Op.SwapN, offset)
        }

        case Op.MPut => {
          val offset = code.size
          code += VM.MPUT
          (Op.MPut, offset)
        }

        case Op.MGet => {
          val offset = code.size
          code += VM.MGET
          (Op.MGet, offset)
        }

        case Op.Add => {
          val offset = code.size
          code += VM.ADD
          (Op.Add, offset)
        }

        case Op.Mul => {
          val offset = code.size
          code += VM.MUL
          (Op.Mul, offset)
        }

        case Op.Div => {
          val offset = code.size
          code += VM.DIV
          (Op.Div, offset)
        }

        case Op.Mod => {
          val offset = code.size
          code += VM.MOD
          (Op.Mod, offset)
        }

        case Op.Lt => {
          val offset = code.size
          code += VM.LT
          (Op.Lt, offset)
        }

        case Op.Gt => {
          val offset = code.size
          code += VM.GT
          (Op.Gt, offset)
        }

        case Op.Eq => {
          val offset = code.size
          code += VM.EQ
          (Op.Eq, offset)
        }

        case Op.Concat => {
          val offset = code.size
          code += VM.CONCAT
          (Op.Concat, offset)
        }

        case Op.Not => {
          val offset = code.size
          code += VM.NOT
          (Op.Not, offset)
        }

        case Op.FAdd => {
          val offset = code.size
          code += VM.FADD
          (Op.FAdd, offset)
        }

        case Op.FMul => {
          val offset = code.size
          code += VM.FMUL
          (Op.FMul, offset)
        }

        case Op.FDiv => {
          val offset = code.size
          code += VM.FDIV
          (Op.FDiv, offset)
        }

        case Op.FMod => {
          val offset = code.size
          code += VM.FMOD
          (Op.FMod, offset)
        }

        case Op.Dupn => {
          val offset = code.size
          code += VM.DUPN
          (Op.Dupn, offset)
        }

        case Op.From => {
          val offset = code.size
          code += VM.FROM
          (Op.From, offset)
        }

        case Op.PCreate => {
          val offset = code.size
          code += VM.PCREATE
          (Op.PCreate, offset)
        }

        case Op.PUpdate => {
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

        case Op.Nop => {
          val offset = code.size
          (Op.Nop, offset)
        }

        case Op.Transfer => {
          val offset = code.size
          code += VM.TRANSFER
          (Op.Transfer, offset)
        }

        case Op.Label(n) => {
          val offset = code.size
          (Op.Label(n), offset)
        }
      }
      .collect { case (Op.Label(n), v) => (n, v) }
      .toMap
    omap
  }

  def gen(unit: Seq[Op]): Array[Byte] = {

    val offset = offsets(unit)
    val code = new ArrayBuffer[Byte](unit.size)

    unit.foreach {
      case Op.Push(d) => {
        code += VM.PUSHX
        d match {
          case d: Datum.Integral => code ++= vm.int32ToWord(d.value)
          case d: Datum.Floating => code ++= vm.doubleToWord(d.value)
          case d: Datum.Rawbytes => code ++= vm.bytesToWord(d.value)
        }
      }

      case Op.Jump(n) => {
        code += VM.PUSHX
        code ++= vm.int32ToWord(offset(n))
        code += VM.JUMP
      }

      case Op.JumpI(n) => {
        code += VM.PUSHX
        code ++= vm.int32ToWord(offset(n))
        code += VM.JUMPI
      }

      case Op.Call(n) => {
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

      case Op.SGet  => code += VM.SGET
      case Op.SPut  => code += VM.SPUT
      case Op.SExst => code += VM.SEXIST

      case Op.Label(n) =>
      case Op.Stop     => code += VM.STOP
      case Op.Pop      => code += VM.POP
      case Op.Dup      => code += VM.DUP
      case Op.Swap     => code += VM.SWAP
      case Op.SwapN    => code += VM.SWAPN
      case Op.Ret      => code += VM.RET
      case Op.MPut     => code += VM.PRIMITIVE_PUT
      case Op.MGet     => code += VM.PRIMITIVE_GET
      case Op.Add   => code += VM.ADD
      case Op.Mul   => code += VM.MUL
      case Op.Div   => code += VM.DIV
      case Op.Mod   => code += VM.MOD
      case Op.Lt    => code += VM.LT
      case Op.Gt    => code += VM.GT
      case Op.Eq       => code += VM.EQ
      case Op.Not      => code += VM.NOT
      case Op.Dupn     => code += VM.DUPN
      case Op.From     => code += VM.FROM
      case Op.PCreate  => code += VM.PCREATE
      case Op.PUpdate  => code += VM.PUPDATE
      case Op.Transfer => code += VM.TRANSFER
      case Op.Nop      =>
    }

    code.toArray
  }

  def ungen(unit: ByteString): Seq[(Int, Op)] = {
    ungen(unit.toByteArray)
  }

  def ungen(unit: Array[Byte]): Seq[(Int, Op)] = {

    import java.nio.ByteBuffer
    import vm._

    val ubuf = ByteBuffer.wrap(unit)
    val obuf = new ArrayBuffer[(Int, Op)]()

    while (ubuf.remaining > 0) {
      val pos = ubuf.position()
      val ins = ubuf.get()
      ins & 0xFF match {
        case VM.int.PUSHX    => obuf += ((pos, Op.Push(Datum.Rawbytes(wordToBytes(ubuf)))))
        case VM.int.CALL     => obuf += ((pos, Op.Call("")))
        case VM.int.STOP     => obuf += ((pos, Op.Stop))
        case VM.int.JUMP     => obuf += ((pos, Op.Jump("")))
        case VM.int.JUMPI    => obuf += ((pos, Op.JumpI("")))
        case VM.int.POP      => obuf += ((pos, Op.Pop))
        case VM.int.DUP      => obuf += ((pos, Op.Dup))
        case VM.int.SWAP     => obuf += ((pos, Op.Swap))
        case VM.int.SWAPN    => obuf += ((pos, Op.SwapN))
        case VM.int.RET      => obuf += ((pos, Op.Ret))
        case VM.int.PRIMITIVE_PUT     => obuf += ((pos, Op.MPut))
        case VM.int.PRIMITIVE_GET     => obuf += ((pos, Op.MGet))
        case VM.int.ADD   => obuf += ((pos, Op.Add))
        case VM.int.MUL   => obuf += ((pos, Op.Mul))
        case VM.int.DIV   => obuf += ((pos, Op.Div))
        case VM.int.MOD   => obuf += ((pos, Op.Mod))
        case VM.int.LT    => obuf += ((pos, Op.Lt))
        case VM.int.GT    => obuf += ((pos, Op.Gt))
        case VM.int.NOT      => obuf += ((pos, Op.Not))
        case VM.int.EQ       => obuf += ((pos, Op.Eq))
        case VM.int.DUPN     => obuf += ((pos, Op.Dupn))
        case VM.int.FROM     => obuf += ((pos, Op.From))
        case VM.int.PCREATE  => obuf += ((pos, Op.PCreate))
        case VM.int.PUPDATE  => obuf += ((pos, Op.PUpdate))
        case VM.int.PCALL    => obuf += ((pos, Op.PCall))
        case VM.int.TRANSFER => obuf += ((pos, Op.Transfer))
        case VM.int.LCALL => obuf += ((pos, Op.LCall))
        case VM.int.SGET   => obuf += ((pos, Op.SGet))
        case VM.int.SPUT   => obuf += ((pos, Op.SPut))
        case VM.int.SEXIST => obuf += ((pos, Op.SExst))
      }
    }

    obuf
  }

}

object ByteCode {
  def apply(): ByteCode = new ByteCode
}
