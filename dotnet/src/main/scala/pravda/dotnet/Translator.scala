package pravda.dotnet

import java.nio.charset.StandardCharsets

import pravda.dotnet.CIL._
import pravda.dotnet.Signatures.SigType.{Cls, Generic}
import pravda.dotnet.Signatures._
import pravda.dotnet.TablesData._
import pravda.vm.asm._
import pravda.vm.serialization._

object Translator {
  private def resolveRVI(opcodes: Seq[OpCode]): Seq[OpCode] = {

    val offsets = opcodes
      .foldLeft((0, Set.empty[Int])) {
        case ((curOffset, offsets), opcode) =>
          val newOffsets = opcode match {
            case BrS(t) if t != 0      => Seq(curOffset + t + 2)
            case BrFalseS(t) if t != 0 => Seq(curOffset + t + 2)
            case BrTrueS(t) if t != 0  => Seq(curOffset + t + 2)
            //case Switch(ts)            => ts.filter(_ != 0).map(_ + curOffset + 1)
            case _ => Seq.empty
          }
          (curOffset + opcode.size, offsets ++ newOffsets)
      }
      ._2

    def mkLabel(i: Int): String = "br" + i.toString

    val opcodesWithLabels = opcodes
      .foldLeft((0, Seq.empty[OpCode])) {
        case ((curOffset, opcodes), opcode) =>
          val newOpcodes = opcode match {
            case BrS(0)      => List(Nop)
            case BrTrueS(0)  => List(Nop)
            case BrFalseS(0) => List(Nop)
            case BrS(t)      => List(Jump(mkLabel(curOffset + t + 2)))
            case BrFalseS(t) => List(Not, JumpI(mkLabel(curOffset + t + 2)))
            case BrTrueS(t)  => List(JumpI(mkLabel(curOffset + t + 2)))
            //case Switch(ts) => ts.filter(_ != 0).map(t => Label(mkLabel(curOffset + t + 1))) // FIXME switch
            case opcode if offsets.contains(curOffset) => List(Label(mkLabel(curOffset)), opcode)
            case opcode                                => List(opcode)
          }
          (curOffset + opcode.size, opcodes ++ newOpcodes)
      }
      ._2

    opcodesWithLabels
  }

  private def translateMethod(argsCount: Int,
                              localsCount: Int,
                              name: String,
                              opcodes: Seq[OpCode],
                              signatures: Map[Long, Signatures.Signature],
                              local: Boolean): Seq[Op] = {

    def translateOpcode(opcode: OpCode, stackOffest: Int): (Int, Seq[Op]) = {
      def pushTypedInt(i: Int): Op =
        Op.Push(Datum.Rawbytes(Array(1.toByte) ++ int32ToData(i).toByteArray))

      def pushTypedFloat(d: Double): Op =
        Op.Push(Datum.Rawbytes(Array(2.toByte) ++ doubleToData(d).toByteArray))

      def computeLocalOffset(num: Int): Int =
        (localsCount - num - 1) + stackOffest + 1

      def computeArgOffset(num: Int): Int =
        (argsCount - num - 1) + stackOffest + localsCount + 1 + 1
      // for local there's additional object arg
      // for not local there's name of the method

      def storeLocal(num: Int): Seq[Op] =
        Seq(
          Op.Push(Datum.Integral(computeLocalOffset(num))),
          Op.SwapN,
          Op.Pop
        )

      def loadLocal(num: Int): Seq[Op] =
        Seq(
          Op.Push(Datum.Integral(computeLocalOffset(num))),
          Op.Dupn
        )

      def loadArg(num: Int): (Int, Seq[Op]) =
        if (local) {
          (1,
            Seq(
              Op.Push(Datum.Integral(computeArgOffset(num))),
              Op.Dupn
            ))
        } else {
          if (num == 0) {
            (0, Seq.empty) // skip this reference
          } else {
            (1,
              Seq(
                Op.Push(Datum.Integral(computeArgOffset(num - 1))),
                Op.Dupn
              ))
          }
        }

      def loadField(name: String, sigIdx: Long): (Int, Seq[Op]) = { // FIXME should process static fields too
        lazy val defaultLoad = (1,
                                Seq(
                                  Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
                                  Op.SGet
                                ))

        signatures.get(sigIdx) match {
          case Some(FieldSig(tpe)) =>
            tpe match {
              case SigType.Generic(SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)), _) =>
                (1, Seq(Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8)))))
              case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) =>
                (1, Seq(Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8)))))
              case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) if name == "sender" =>
                (1, Seq(Op.From))
              case _ => defaultLoad
            }
          case _ => defaultLoad
        }
      }

      def storeField(name: String, sigIdx: Long): (Int, Seq[Op]) = { // FIXME should process static fields too
        lazy val defaultStore = (1,
                                 Seq(
                                   Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
                                   Op.SPut
                                 ))

        signatures.get(sigIdx) match {
          case Some(FieldSig(tpe)) =>
            tpe match {
              case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) =>
                (0, Seq(Op.Stop)) // error user shouldn't modify mappings
              case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) if name == "sender" =>
                (0, Seq(Op.Stop)) // error user shouldn't modify sender address
              case _ => defaultStore
            }
          case _ => defaultStore
        }
      }

      def callVirt(name: String, parentSigIdx: Long, methodSigIdx: Long): (Int, Seq[Op]) = {
        val resO = for {
          parentSig <- signatures.get(parentSigIdx)
          methodSig <- signatures.get(methodSigIdx)
        } yield {
          lazy val defaultCall = methodSig match {
            case MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), params) =>
              tpe match {
                case SigType.Void => (-params.length, Seq.fill(params.length)(Op.Pop))
                case _ =>
                  (-params.length + 1, Seq.fill(params.length)(Op.Pop) :+ Op.Push(Datum.Rawbytes(Array[Byte](0))))
              }
            case _ => (0, Seq.empty)
          }

          parentSig match {
            case TypeSig(Tpe(Generic(Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)), _), _)) =>
              name match {
                case "get" => (-1, Seq(Op.Swap, Op.Concat, Op.SGet))
                case "getDefault" =>
                  (-2, Seq(Op.Call("method_getDefault")))
                // FIXME we need some way to distinguish local functions and methods of program
                case "exists" => (-1, Seq(Op.Swap, Op.Concat, Op.SExst))
                case "put" =>
                  (-3,
                   Seq(Op.Push(Datum.Integral(2)),
                       Op.Dupn,
                       Op.Push(Datum.Integral(4)),
                       Op.Dupn,
                       Op.Concat,
                       Op.Swap,
                       Op.SPut,
                       Op.Pop,
                       Op.Pop))
              }
            case _ => defaultCall
          }
        }

        resO.getOrElse((0, Seq.empty))
      }

//      def storeArg(num: Int): Seq[Op] =
//        Seq(
//          Op.Push(Datum.Integral((argsCount - num - 1) + stackOffest + localsCount + 1 + 1)),
//          Op.SwapN,
//          Op.Pop
//        )
      // FIXME when we store args?

      opcode match {
        case LdcI40     => (1, Seq(pushTypedInt(0)))
        case LdcI41     => (1, Seq(pushTypedInt(1)))
        case LdcI42     => (1, Seq(pushTypedInt(2)))
        case LdcI43     => (1, Seq(pushTypedInt(3)))
        case LdcI44     => (1, Seq(pushTypedInt(4)))
        case LdcI45     => (1, Seq(pushTypedInt(5)))
        case LdcI46     => (1, Seq(pushTypedInt(6)))
        case LdcI47     => (1, Seq(pushTypedInt(7)))
        case LdcI4M1    => (1, Seq(pushTypedInt(-1)))
        case LdcI4(num) => (1, Seq(pushTypedInt(num)))
        case LdcI4S(v)  => (1, Seq(pushTypedInt(v.toInt)))
        case LdcR4(f)   => (1, Seq(pushTypedFloat(f.toDouble)))
        case LdcR8(d)   => (1, Seq(pushTypedFloat(d)))
        case Add        => (-1, Seq(Op.LCall("Typed", "typedAdd", 2)))
        case Mull       => (-1, Seq(Op.LCall("Typed", "typedMull", 2)))
        case Div        => (-1, Seq(Op.LCall("Typed", "typedDiv", 2)))
        case Rem        => (-1, Seq(Op.LCall("Typed", "typedMod", 2)))
        case Clt        => (-1, Seq(Op.LCall("Typed", "typedClt", 2)))
        case Sub =>
          (-1, Seq(Op.Swap, pushTypedInt(-1), Op.LCall("Typed", "typedMull", 2), Op.LCall("Typed", "typedAdd", 2)))
        case Cgt => (-1, Seq(Op.Swap, Op.LCall("Typed", "typedClt", 2)))
        case Ceq => (-1, Seq(Op.Eq))
        case Not => (0, Seq(Op.Not))

        case LdSFld(FieldData(_, name, sig)) =>
          loadField(name, sig)
        case LdFld(FieldData(_, name, sig)) =>
          loadField(name, sig)
        case StSFld(FieldData(_, name, sig)) =>
          storeField(name, sig)
        case StFld(FieldData(_, name, sig)) =>
          storeField(name, sig)

        case LdArg0      => loadArg(0)
        case LdArg1      => loadArg(1)
        case LdArg2      => loadArg(2)
        case LdArg3      => loadArg(3)
        case LdArg(num)  => loadArg(num)
        case LdArgS(num) => loadArg(num.toInt)

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

        case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
          callVirt(name, parentSigIdx, methodSigIdx)

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

    Seq(Op.Label("method_" + name)) ++
      Seq.fill(localsCount)(Op.Push(Datum.Integral(0))) ++ // FIXME Should be replaced by proper value for local var type
      ops ++
      (if (local) Seq(Op.Ret) else Seq(Op.Jump("stop")))
  }

  def translate(rawMethods: Seq[Method], cilData: CilData, signatures: Map[Long, Signatures.Signature]): Seq[Op] = {
    val methodsToTypes: Map[Int, TypeDefData] = cilData.tables.methodDefTable.zipWithIndex.flatMap {
      case (m, i) => cilData.tables.typeDefTable.find(_.methods.exists(_ eq m)).map(i -> _)
    }.toMap

    def isLocal(methodIdx: Int): Boolean = {
      methodsToTypes.get(methodIdx) match {
        case Some(TypeDefData(_, _, "io.mytc.pravda", _, _, _)) => true
        case _                                                  => false
      }
    }

    val methods = rawMethods.zipWithIndex.filterNot {
      case (m, i) =>
        val name = cilData.tables.methodDefTable(i).name
        name == ".ctor" || name == ".cctor"
    }

    val jumpToMethod = methods.flatMap {
      case (m, i) =>
        if (!isLocal(i)) {
          val name = cilData.tables.methodDefTable(i).name
          Seq(
            Op.Dup,
            Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
            Op.Eq,
            Op.JumpI("method_" + name)
          )
        } else {
          Seq.empty
        }
    }

    val methodsOps = methods.flatMap {
      case (m, i) =>
        val localVarSig = m.localVarSigIdx.flatMap(signatures.get)
        translateMethod(
          cilData.tables.methodDefTable(i).params.length, // FIXME should properly handle static methods
          localVarSig
            .map {
              case LocalVarSig(types) => types.length
              case _                  => 0
            }
            .getOrElse(0),
          cilData.tables.methodDefTable(i).name,
          resolveRVI(m.opcodes),
          signatures,
          isLocal(i)
        )
    }

    jumpToMethod ++ Seq(Op.Jump("stop")) ++ methodsOps ++ Seq(Op.Label("stop"))
  }
}
