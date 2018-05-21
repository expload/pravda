package io.mytc.sood.cil

import java.nio.charset.StandardCharsets

import pravda.vm.asm._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import pravda.vm._
import serialization._

object Translator {
  final case class CilContext(opcodes: Seq[OpCode])

  def resolveRVI(ctx: CilContext): CilContext = {

    def mkLabel(i: Int): String = i.toString

    val labels = ctx.opcodes.zipWithIndex.collect {
      case (o @ BrS(t), i) if t != 0      => i + t
      case (o @ BrFalseS(t), i) if t != 0 => i + t
      case (o @ BrTrueS(t), i) if t != 0  => i + t
    }.toSet

    val opcodes = ctx.opcodes.zipWithIndex.flatMap {
      case (BrS(0), i)           => List(Nop)
      case (BrTrueS(0), i)       => List(Nop)
      case (BrFalseS(0), i)      => List(Nop)
      case (BrS(t), i) if t != 0 => List(Jump(mkLabel(i + t)))
      case (BrFalseS(t), i) if t != 0 =>
        List(
          Not,
          JumpI(mkLabel(i + t))
        )
      case (BrTrueS(t), i) if t != 0 =>
        List(
          JumpI(mkLabel(i + t))
        )
      case (opcode, i) if labels.contains(i) => List(Label(mkLabel(i)), opcode)
      case (opcode, i)                       => List(opcode)
    }

    ctx.copy(opcodes = opcodes)
  }

  def translate(ctx: CilContext): Seq[Op] = {

    def pushTypedInt(i: Int): Op =
      Op.Push(Datum.Rawbytes(Array(1.toByte) ++ int32ToData(i).toByteArray))

    def pushTypedFloat(d: Double): Op =
      Op.Push(Datum.Rawbytes(Array(2.toByte) ++ doubleToData(d).toByteArray))

    def storeLocal(num: Int): Seq[Op] =
      Seq(
        Op.Push(Datum.Integral(num)),
        Op.LCall("Local", "storeLocal", 2)
      )

    def loadLocal(num: Int): Seq[Op] =
      Seq(
        Op.Push(Datum.Integral(num)),
        Op.LCall("Local", "loadLocal", 1)
      )

    resolveRVI(ctx).opcodes.flatMap {
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
      case Clt       => Seq(Op.LCall("Typed", "typedClt", 2))

      case LdSFld(FieldData(_, name, _)) =>
        Seq(
          // more to come...
          Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
          Op.LCall("Classes", "loadField", 1)
        )

      case StLoc0      => storeLocal(0)
      case StLoc1      => storeLocal(1)
      case StLoc2      => storeLocal(2)
      case StLoc3      => storeLocal(3)
      case StLoc(num)  => storeLocal(num)
      case StLocS(num) => storeLocal(num.toInt)

      case LdLoc0      => loadLocal(0)
      case LdLoc1      => loadLocal(1)
      case LdLoc2      => loadLocal(2)
      case LdLoc3      => loadLocal(3)
      case LdLoc(num)  => loadLocal(num)
      case LdLocS(num) => loadLocal(num.toInt)

      case Nop          => Seq(Op.Nop)
      case Ret          => Seq(Op.Ret)
      case Jump(label)  => Seq(Op.Jump(label))
      case JumpI(label) => Seq(Op.JumpI(label))
      case Label(label) => Seq(Op.Label(label))

      case _ => Seq.empty
    }
  }
}
