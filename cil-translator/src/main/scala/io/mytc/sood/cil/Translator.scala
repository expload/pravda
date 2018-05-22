package io.mytc.sood.cil

import java.nio.charset.StandardCharsets

import pravda.vm.asm._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import pravda.vm._
import serialization._

object Translator {
  private def resolveRVI(opcodes: Seq[OpCode]): Seq[OpCode] = {

    val offsets = opcodes.foldLeft((0, Set.empty[Int])) {
      case ((curOffset, offsets), opcode) =>
        val newOffsets = opcode match {
          case BrS(t) if t != 0      => Seq(curOffset + t + 2)
          case BrFalseS(t) if t != 0 => Seq(curOffset + t + 2)
          case BrTrueS(t) if t != 0  => Seq(curOffset + t + 2)
          //case Switch(ts)            => ts.filter(_ != 0).map(_ + curOffset + 1)
          case _                     => Seq.empty
        }
        (curOffset + opcode.size, offsets ++ newOffsets)
    }._2

    def mkLabel(i: Int): String = "br" + i.toString

    val opcodesWithLabels = opcodes.foldLeft((0, Seq.empty[OpCode])) {
      case ((curOffset, opcodes), opcode) =>
        val newOpcodes = opcode match {
          case BrS(0) => List(Nop)
          case BrTrueS(0) => List(Nop)
          case BrFalseS(0) => List(Nop)
          case BrS(t) => List(Jump(mkLabel(curOffset + t + 2)))
          case BrFalseS(t) => List(Not, JumpI(mkLabel(curOffset + t + 2)))
          case BrTrueS(t) => List(JumpI(mkLabel(curOffset + t + 2)))
          //case Switch(ts) => ts.filter(_ != 0).map(t => Label(mkLabel(curOffset + t + 1))) // FIXME switch
          case opcode if offsets.contains(curOffset) => List(Label(mkLabel(curOffset)), opcode)
          case opcode => List(opcode)
        }
        (curOffset + opcode.size, opcodes ++ newOpcodes)
    }._2

    opcodesWithLabels
  }

  private def translateMethod(argsCount: Int, localsCount: Int, name: String, opcodes: Seq[OpCode]): Seq[Op] = {
    def translateOpcode(opcode: OpCode, stackOffest: Int): (Int, Seq[Op]) = {
      def pushTypedInt(i: Int): Op =
        Op.Push(Datum.Rawbytes(Array(1.toByte) ++ int32ToData(i).toByteArray))

      def pushTypedFloat(d: Double): Op =
        Op.Push(Datum.Rawbytes(Array(2.toByte) ++ doubleToData(d).toByteArray))

      def storeLocal(num: Int): Seq[Op] =
        Seq(
          Op.Push(Datum.Integral((localsCount - num - 1) + stackOffest + 1)),
          Op.SwapN,
          Op.Pop
        )

      def loadLocal(num: Int): Seq[Op] =
        Seq(
          Op.Push(Datum.Integral((localsCount - num - 1) + stackOffest + 1)),
          Op.Dupn
        )

      def loadArg(num: Int): Seq[Op] =
        Seq(
          Op.Push(Datum.Integral((argsCount - num - 1) + stackOffest + localsCount + 1 + 1)),
          Op.Dupn
        )

//      def storeArg(num: Int): Seq[Op] =
//        Seq(
//          Op.Push(Datum.Integral((argsCount - num - 1) + stackOffest + localsCount + 1 + 1)),
//          Op.SwapN,
//          Op.Pop
//        )
      // FIXME when we store args?

      opcode match {
        case LdcI40    => (1, Seq(pushTypedInt(0)))
        case LdcI41    => (1, Seq(pushTypedInt(1)))
        case LdcI42    => (1, Seq(pushTypedInt(2)))
        case LdcI43    => (1, Seq(pushTypedInt(3)))
        case LdcI44    => (1, Seq(pushTypedInt(4)))
        case LdcI45    => (1, Seq(pushTypedInt(5)))
        case LdcI46    => (1, Seq(pushTypedInt(6)))
        case LdcI47    => (1, Seq(pushTypedInt(7)))
        case LdcI4M1   => (1, Seq(pushTypedInt(-1)))
        case LdcI4(num) => (1, Seq(pushTypedInt(num)))
        case LdcI4S(v) => (1, Seq(pushTypedInt(v.toInt)))
        case LdcR4(f)  => (1, Seq(pushTypedFloat(f.toDouble)))
        case LdcR8(d)  => (1, Seq(pushTypedFloat(d)))
        case Add       => (-1, Seq(Op.LCall("Typed", "typedAdd", 2)))
        case Mull      => (-1, Seq(Op.LCall("Typed", "typedMull", 2)))
        case Div       => (-1, Seq(Op.LCall("Typed", "typedDiv", 2)))
        case Rem       => (-1, Seq(Op.LCall("Typed", "typedMod", 2)))
        case Clt       => (-1, Seq(Op.LCall("Typed", "typedClt", 2)))
        case Cgt       => (-1, Seq(Op.Swap, Op.LCall("Typed", "typedClt", 2)))
        case Not       => (0, Seq(Op.Not))

        case LdSFld(FieldData(_, name, _)) =>
          (1,
           Seq(
             Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
             Op.SGet
           ))
        case LdFld(FieldData(_, name, _)) =>
          (1,
           Seq(
             Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
             Op.SGet
           ))
        case StSFld(FieldData(_, name, _)) =>
          (-1,
           Seq(
             Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
             Op.SPut
           ))
        case StFld(FieldData(_, name, _)) =>
          (-1,
           Seq(
             Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
             Op.SPut
           ))

        case LdArg0      => (1, loadArg(0))
        case LdArg1      => (1, loadArg(1))
        case LdArg2      => (1, loadArg(2))
        case LdArg3      => (1, loadArg(3))
        case LdArg(num)  => (1, loadArg(num))
        case LdArgS(num) => (1, loadArg(num.toInt))

        case StLoc0      => (-1, storeLocal(0))
        case StLoc1      => (-1, storeLocal(1))
        case StLoc2      => (-1, storeLocal(2))
        case StLoc3      => (-1, storeLocal(3))
        case StLoc(num)  => (-1, storeLocal(num))
        case StLocS(num) => (-1, storeLocal(num.toInt))

        case LdLoc0      => (1, loadLocal(0))
        case LdLoc1      => (1, loadLocal(1))
        case LdLoc2      => (1, loadLocal(2))
        case LdLoc3      => (1, loadLocal(3))
        case LdLoc(num)  => (1, loadLocal(num))
        case LdLocS(num) => (1, loadLocal(num.toInt))

        case Nop => (0, Seq(Op.Nop))
        //case Ret          => (0, Seq(Op.Ret))
        case Jump(label)  => (0, Seq(Op.Jump(label)))
        case JumpI(label) => (-1, Seq(Op.JumpI(label)))
        case Label(label) => (0, Seq(Op.Label(label)))

        case _ => (0, Seq.empty)
      }
    }

    val ops = opcodes
      .foldLeft((Seq.empty[Op], 0)) {
        case ((res, stackOffset), op) =>
          val (deltaOffset, opcode) = translateOpcode(op, stackOffset)
          (res ++ opcode, stackOffset + deltaOffset)
      }
      ._1

    Seq(Op.Label("method" + name)) ++
      Seq.fill(localsCount)(Op.Push(Datum.Integral(0))) ++ // FIXME Should be replaced by proper value for local var type
      ops ++
      Seq(Op.Jump("stop"))
  }

  def translate(methods: Seq[Method], cilData: CilData): Seq[Op] = {
    val jumpToMethod = methods.zipWithIndex.flatMap {
      case (m, i) =>
        val name = cilData.tables.methodDefTable(i).name
        Seq(
          Op.Dup,
          Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
          Op.Eq,
          Op.JumpI("method" + name)
        )
    }

    val methodsOps = methods.zipWithIndex.flatMap {
      case (m, i) =>
        translateMethod(cilData.tables.methodDefTable(i).params.length,
                        m.localVarSig.types.length,
                        cilData.tables.methodDefTable(i).name,
                        resolveRVI(m.opcodes))
    }

    jumpToMethod ++ Seq(Op.Jump("stop")) ++ methodsOps ++ Seq(Op.Label("stop"))
  }
}
