package io.mytc.sood.cil

import io.mytc.sood.asm._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.vm._

object Translator {
  final case class CilContext(opcodes: Seq[OpCode])

  def translate(ctx: CilContext): Seq[Op] = {

    def pushTypedInt(i: Int): Op =
      Op.Push(Datum.Rawbytes(1.toByte +: int32ToWord(i)))

    def pushTypedFloat(d: Double): Op =
      Op.Push(Datum.Rawbytes(2.toByte +: doubleToWord(d)))

    ctx.opcodes.flatMap {
      case LdcI40    => Seq(pushTypedInt(0))
      case LdcI41    => Seq(pushTypedInt(1))
      case LdcI42    => Seq(pushTypedInt(2))
      case LdcI43    => Seq(pushTypedInt(3))
      case LdcI44    => Seq(pushTypedInt(4))
      case LdcI45    => Seq(pushTypedInt(5))
      case LdcI46    => Seq(pushTypedInt(6))
      case LdcI47    => Seq(pushTypedInt(7))
      case LdcI4M1   => Seq(pushTypedInt(-1))
      case LdcI4S(v) => Seq(pushTypedInt(v.toInt))
      case LdcR4(f)  => Seq(pushTypedFloat(f.toDouble))
      case LdcR8(d)  => Seq(pushTypedFloat(d))
      case Add       => Seq(Op.LCall("Typed", "typedAdd", 2))
      case Mull      => Seq(Op.LCall("Typed", "typedMull", 2))
      case Div       => Seq(Op.LCall("Typed", "typedDiv", 2))
      case Rem       => Seq(Op.LCall("Typed", "typedMod", 2))
      case Nop => Seq.empty
      case _ => Seq.empty
    }
  }
}
