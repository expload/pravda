package pravda.dotnet.translation

import java.nio.charset.StandardCharsets

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import pravda.vm.asm._
import pravda.vm.serialization._

object Translator {

  object Constants {
    val labelStackOffset = 0
    val jumpStackOffset = 0
    val jumpIStackOffset = -1
  }

  private def resolveBranches(opcodes: List[OpCode]): List[OpCode] = {

    val offsets = opcodes
      .foldLeft((0, List.empty[Int])) {
        case ((curOffset, offsets), opcode) =>
          val newOffsets = opcode match {
            case BrS(t) if t != 0 => List(curOffset + t + 2)
            case Br(t) if t != 0  => List(curOffset + t + 5)
            case BrFalseS(t)      => List(curOffset + t + 2)
            case BrFalse(t)       => List(curOffset + t + 5)
            case BrTrueS(t)       => List(curOffset + t + 2)
            case BrTrue(t)        => List(curOffset + t + 5)
            case BltS(t)          => List(curOffset + t + 2)
            case Blt(t)           => List(curOffset + t + 5)
            case BgtS(t)          => List(curOffset + t + 2)
            case Bgt(t)           => List(curOffset + t + 5)
            case BleS(t)          => List(curOffset + t + 2)
            case Ble(t)           => List(curOffset + t + 5)
            case BgeS(t)          => List(curOffset + t + 2)
            case Bge(t)           => List(curOffset + t + 5)
            case BeqS(t)          => List(curOffset + t + 2)
            case Beq(t)           => List(curOffset + t + 5)
            //case Switch(ts)            => ts.filter(_ != 0).map(_ + curOffset + 1)
            case _ => List.empty
          }
          (curOffset + opcode.size, offsets ++ newOffsets)
      }
      ._2

    def mkLabel(i: Int): String = "br" + i.toString

    val opcodesWithLabels = opcodes
      .foldLeft((0, List.empty[OpCode])) {
        case ((curOffset, opcodes), opcode) =>
          val newOpcodes = opcode match {
            case BrS(0)      => List(Nop)
            case BrS(t)      => List(Jump(mkLabel(curOffset + t + 2)))
            case Br(t)       => List(Jump(mkLabel(curOffset + t + 5)))
            case BrFalseS(t) => List(Not, JumpI(mkLabel(curOffset + t + 2)))
            case BrFalse(t)  => List(Not, JumpI(mkLabel(curOffset + t + 5)))
            case BrTrueS(t)  => List(JumpI(mkLabel(curOffset + t + 2)))
            case BrTrue(t)   => List(JumpI(mkLabel(curOffset + t + 5)))
            case BltS(t)     => List(Clt, JumpI(mkLabel(curOffset + t + 2)))
            case Blt(t)      => List(Clt, JumpI(mkLabel(curOffset + t + 5)))
            case BgtS(t)     => List(Cgt, JumpI(mkLabel(curOffset + t + 2)))
            case Bgt(t)      => List(Cgt, JumpI(mkLabel(curOffset + t + 5)))
            case BleS(t)     => List(Cgt, Not, JumpI(mkLabel(curOffset + t + 2)))
            case Ble(t)      => List(Cgt, Not, JumpI(mkLabel(curOffset + t + 5)))
            case BgeS(t)     => List(Clt, Not, JumpI(mkLabel(curOffset + t + 2)))
            case Bge(t)      => List(Clt, Not, JumpI(mkLabel(curOffset + t + 5)))
            case BeqS(t)     => List(Ceq, JumpI(mkLabel(curOffset + t + 2)))
            case Beq(t)      => List(Ceq, JumpI(mkLabel(curOffset + t + 5)))
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
                              opcodes: List[OpCode],
                              signatures: Map[Long, Signatures.Signature],
                              local: Boolean,
                              void: Boolean): Either[String, List[Op]] = {

    def translateOpcode(opcode: OpCode, stackOffsetO: Option[Int]): Either[String, (Int, List[Op])] = {
      def pushTypedInt(i: Int): Op =
        Op.Push(Datum.Rawbytes(Array(1.toByte) ++ int32ToData(i).toByteArray))

      def pushTypedFloat(d: Double): Op =
        Op.Push(Datum.Rawbytes(Array(2.toByte) ++ doubleToData(d).toByteArray))

      def computeLocalOffset(num: Int, stackOffset: Int): Int =
        (localsCount - num - 1) + stackOffset + 1

      def computeArgOffset(num: Int, stackOffset: Int): Int =
        (argsCount - num - 1) + stackOffset + localsCount + 1 + 1
      // for local there's additional object arg
      // for not local there's name of the method

      def storeLocal(num: Int): Either[String, List[Op]] =
        stackOffsetO
          .map { s =>
            Right(
              List(
                Op.Push(Datum.Integral(computeLocalOffset(num, s))),
                Op.SwapN,
                Op.Pop
              ))
          }
          .getOrElse(Left("Stack offset is required for storing local variables"))

      def loadLocal(num: Int): Either[String, List[Op]] =
        stackOffsetO
          .map { s =>
            Right(
              List(
                Op.Push(Datum.Integral(computeLocalOffset(num, s))),
                Op.Dupn
              ))
          }
          .getOrElse(Left("Stack offset is required for loading local variables"))

      def loadArg(num: Int): Either[String, (Int, List[Op])] =
        stackOffsetO
          .map { s =>
            if (local) {
              Right(
                (1,
                 List(
                   Op.Push(Datum.Integral(computeArgOffset(num, s))),
                   Op.Dupn
                 )))
            } else {
              if (num == 0) {
                Right((0, List.empty)) // skip this reference
              } else {
                Right(
                  (1,
                   List(
                     Op.Push(Datum.Integral(computeArgOffset(num - 1, s))),
                     Op.Dupn
                   )))
              }
            }
          }
          .getOrElse(Left("Stack offset is required for arguments loading"))

      def loadField(name: String, sigIdx: Long): (Int, List[Op]) = { // FIXME should process static fields too
        lazy val defaultLoad = (1,
                                List(
                                  Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
                                  Op.SGet
                                ))

        signatures.get(sigIdx) match {
          case Some(FieldSig(tpe)) =>
            tpe match {
              case SigType.Generic(SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)), _) =>
                (1, List(Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8)))))
              case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) =>
                (1, List(Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8)))))
              case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) if name == "sender" =>
                (1, List(Op.From))
              case _ => defaultLoad
            }
          case _ => defaultLoad
        }
      }

      def storeField(name: String, sigIdx: Long): (Int, List[Op]) = { // FIXME should process static fields too
        lazy val defaultStore = (1,
                                 List(
                                   Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
                                   Op.SPut
                                 ))

        signatures.get(sigIdx) match {
          case Some(FieldSig(tpe)) =>
            tpe match {
              case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) =>
                (0, List(Op.Stop)) // error user shouldn't modify mappings
              case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) if name == "sender" =>
                (0, List(Op.Stop)) // error user shouldn't modify sender address
              case _ => defaultStore
            }
          case _ => defaultStore
        }
      }

      def callVirt(name: String, parentSigIdx: Long, methodSigIdx: Long): (Int, List[Op]) = {
        val resO = for {
          parentSig <- signatures.get(parentSigIdx)
          methodSig <- signatures.get(methodSigIdx)
        } yield {
          lazy val defaultCall = methodSig match {
            case MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), params) =>
              tpe match {
                case SigType.Void => (-params.length, List.fill(params.length)(Op.Pop))
                case _ =>
                  (-params.length + 1, List.fill(params.length)(Op.Pop) :+ Op.Push(Datum.Rawbytes(Array[Byte](0))))
              }
            case _ => (0, List.empty)
          }

          parentSig match {
            case TypeSig(Tpe(Generic(Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)), _), _)) =>
              name match {
                case "get" => (-1, List(Op.Swap, Op.Concat, Op.SGet))
                case "getDefault" =>
                  (-2, List(Op.Call("method_getDefault")))
                // FIXME we need some way to distinguish local functions and methods of program
                case "exists" => (-1, List(Op.Swap, Op.Concat, Op.SExst, Op.LCall("Typed", "typedBool", 1)))
                case "put" =>
                  (-3,
                   List(Op.Push(Datum.Integral(2)),
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

        resO.getOrElse((0, List.empty))
      }

      val translate: PartialFunction[OpCode, Either[String, (Int, List[Op])]] = {
        case LdcI40     => Right((1, List(pushTypedInt(0))))
        case LdcI41     => Right((1, List(pushTypedInt(1))))
        case LdcI42     => Right((1, List(pushTypedInt(2))))
        case LdcI43     => Right((1, List(pushTypedInt(3))))
        case LdcI44     => Right((1, List(pushTypedInt(4))))
        case LdcI45     => Right((1, List(pushTypedInt(5))))
        case LdcI46     => Right((1, List(pushTypedInt(6))))
        case LdcI47     => Right((1, List(pushTypedInt(7))))
        case LdcI48     => Right((1, List(pushTypedInt(8))))
        case LdcI4M1    => Right((1, List(pushTypedInt(-1))))
        case LdcI4(num) => Right((1, List(pushTypedInt(num))))
        case LdcI4S(v)  => Right((1, List(pushTypedInt(v.toInt))))
        case LdcR4(f)   => Right((1, List(pushTypedFloat(f.toDouble))))
        case LdcR8(d)   => Right((1, List(pushTypedFloat(d))))
        case Add        => Right((-1, List(Op.LCall("Typed", "typedAdd", 2))))
        case Mull       => Right((-1, List(Op.LCall("Typed", "typedMul", 2))))
        case Div        => Right((-1, List(Op.LCall("Typed", "typedDiv", 2))))
        case Rem        => Right((-1, List(Op.LCall("Typed", "typedMod", 2))))
        case Sub =>
          Right((-1, List(pushTypedInt(-1), Op.LCall("Typed", "typedMul", 2), Op.LCall("Typed", "typedAdd", 2))))
        case Clt => Right((-1, List(Op.LCall("Typed", "typedClt", 2))))
        case Cgt => Right((-1, List(Op.Swap, Op.LCall("Typed", "typedClt", 2))))
        case Ceq => Right((-1, List(Op.Eq, Op.LCall("Typed", "typedBool", 1))))
        case Not => Right((0, List(Op.LCall("Typed", "typedNot", 1))))

        case LdSFld(FieldData(_, name, sig)) =>
          Right(loadField(name, sig))
        case LdFld(FieldData(_, name, sig)) =>
          Right(loadField(name, sig))
        case StSFld(FieldData(_, name, sig)) =>
          Right(storeField(name, sig))
        case StFld(FieldData(_, name, sig)) =>
          Right(storeField(name, sig))

        case LdArg0      => loadArg(0)
        case LdArg1      => loadArg(1)
        case LdArg2      => loadArg(2)
        case LdArg3      => loadArg(3)
        case LdArg(num)  => loadArg(num)
        case LdArgS(num) => loadArg(num.toInt)

        case StLoc0      => storeLocal(0).map((-1, _))
        case StLoc1      => storeLocal(1).map((-1, _))
        case StLoc2      => storeLocal(2).map((-1, _))
        case StLoc3      => storeLocal(3).map((-1, _))
        case StLoc(num)  => storeLocal(num).map((-1, _))
        case StLocS(num) => storeLocal(num.toInt).map((-1, _))

        case LdLoc0      => loadLocal(0).map((1, _))
        case LdLoc1      => loadLocal(1).map((1, _))
        case LdLoc2      => loadLocal(2).map((1, _))
        case LdLoc3      => loadLocal(3).map((1, _))
        case LdLoc(num)  => loadLocal(num).map((1, _))
        case LdLocS(num) => loadLocal(num.toInt).map((1, _))

        case Nop          => Right((0, List(Op.Nop)))
        case Ret          => Right((0, List()))
        case Jump(label)  => Right((Constants.jumpStackOffset, List(Op.Jump(label))))
        case JumpI(label) => Right((Constants.jumpIStackOffset, List(pushTypedInt(1), Op.Eq, Op.JumpI(label))))
        case Label(label) => Right((Constants.labelStackOffset, List(Op.Label(label))))

        case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
          Right(callVirt(name, parentSigIdx, methodSigIdx))
      } // FIXME partial function to either

      translate.lift.andThen(_.map(Right(_)).getOrElse(Left(s"Unknown opcode: $opcode")))(opcode).joinRight
    }

    def transformStackOffset(op: OpCode,
                             labelOffsets: Map[String, Int],
                             stackOffsetO: Option[Int]): Either[String, (Map[String, Int], Option[Int])] = {
      val unstableStackError = Left("Unsupported sequence of instructions: stack is unstable")
      def unreachableInstructionsError = Left("Unsupported sequence of instructions: some instructions are unreachable")


      op match {
        case Label(label) =>
          (labelOffsets.get(label), stackOffsetO) match {
            case (Some(offset), Some(stackOffset)) if offset != stackOffset + Constants.labelStackOffset =>
              unstableStackError
            case (Some(offset), Some(stackOffset)) if offset == stackOffset + Constants.labelStackOffset =>
              Right((labelOffsets, Some(stackOffset)))
            case (Some(offset), None)      => Right((labelOffsets, Some(offset)))
            case (None, Some(stackOffset)) => Right((labelOffsets.updated(label, stackOffset), Some(stackOffset)))
            case (None, None)              => unreachableInstructionsError
          }
        case Jump(label) =>
          (labelOffsets.get(label), stackOffsetO) match {
            case (Some(offset), Some(stackOffset)) if offset != stackOffset + Constants.jumpStackOffset =>
              unstableStackError
            case (Some(offset), Some(stackOffset)) if offset == stackOffset + Constants.jumpStackOffset =>
              Right((labelOffsets, None))
            case (None, Some(stackOffset)) =>
              Right((labelOffsets.updated(label, stackOffset + Constants.jumpStackOffset), None))
            case (_, None) => unreachableInstructionsError
          }
        case JumpI(label) =>
          (labelOffsets.get(label), stackOffsetO) match {
            case (Some(offset), Some(stackOffset)) if offset != stackOffset + Constants.jumpIStackOffset =>
              unstableStackError
            case (Some(offset), Some(stackOffset)) if offset == stackOffset + Constants.jumpIStackOffset =>
              Right((labelOffsets, Some(stackOffset)))
            case (None, Some(stackOffset)) =>
              Right((labelOffsets.updated(label, stackOffset + Constants.jumpIStackOffset), Some(stackOffset)))
            case (_, None) => unreachableInstructionsError
          }
        case other =>
          stackOffsetO match {
            case s @ Some(stackOffset) => Right((labelOffsets, s))
            case None                  => unreachableInstructionsError
          }
      }
    }

    val opsE = opcodes
      .foldLeft[Either[String, (List[Op], Map[String, Int], Option[Int])]](
        Right((List.empty[Op], Map.empty[String, Int], Some(0)))) {
        case (Right((res, labelOffsets, stackOffsetO)), op) =>
          for {
            so <- transformStackOffset(op, labelOffsets, stackOffsetO)
            (newLabelOffsets, newStackOffset) = so
            t <- translateOpcode(op, newStackOffset)
            (deltaOffset, opcode) = t
          } yield (res ++ opcode, newLabelOffsets, newStackOffset.map(_ + deltaOffset))
        case (other, op) => other
      }

    val clear =
      if (void) {
        List.fill(localsCount + argsCount + 1)(Op.Pop)
      } else {
        List.fill(localsCount + argsCount + 1)(List(Op.Swap, Op.Pop)).flatten
      }

    for {
      ops <- opsE
    } yield
      List(Op.Label("method_" + name)) ++
        List.fill(localsCount)(Op.Push(Datum.Integral(0))) ++ // FIXME Should be replaced by proper value for local var type
        ops._1 ++
        clear ++
        (if (local) List(Op.Ret) else List(Op.Jump("stop")))
  }

  def translate(rawMethods: List[Method],
                cilData: CilData,
                signatures: Map[Long, Signatures.Signature]): Either[String, List[Op]] = {
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
          List(
            Op.Dup,
            Op.Push(Datum.Rawbytes(name.getBytes(StandardCharsets.UTF_8))),
            Op.Eq,
            Op.JumpI("method_" + name)
          )
        } else {
          List.empty
        }
    }

    val methodsOpsE: Either[String, List[Op]] = methods.map {
      case (m, i) =>
        val localVarSig = m.localVarSigIdx.flatMap(signatures.get)
        cilData.tables.methodDefTable(i) match {
          case MethodDefData(_, _, name, sigIdx, params) =>
            val isVoid = signatures.get(sigIdx) match {
              case Some(MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), params)) =>
                tpe match {
                  case SigType.Void => true
                  case _            => false
                }
              case _ => false
            }

            translateMethod(
              params.length, // FIXME should properly handle static methods
              localVarSig
                .map {
                  case LocalVarSig(types) => types.length
                  case _                  => 0
                }
                .getOrElse(0),
              name,
              resolveBranches(m.opcodes.toList),
              signatures,
              isLocal(i),
              isVoid
            )
        }
    }.flatSequence

    for {
      methodsOps <- methodsOpsE
    } yield jumpToMethod ++ List(Op.Jump("stop")) ++ methodsOps ++ List(Op.Label("stop"))
  }
}
