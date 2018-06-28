package pravda.dotnet.translation

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures
import pravda.dotnet.parsers.Signatures._
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm._

object Translator {

  object Constants {
    val labelStackOffset = 0
    val jumpStackOffset = 0
    val jumpIStackOffset = -1
  }

  final case class OpCodeTranslation(source: Either[String, OpCode], // some name or actual opcode
                                     stackOffset: Option[Int],
                                     asm: List[Operation])
  final case class MethodTranslation(name: String,
                                     argsCount: Int,
                                     localsCount: Int,
                                     local: Boolean,
                                     void: Boolean,
                                     opcodes: List[OpCodeTranslation])
  final case class Translation(jumpToMethods: List[Operation],
                               methods: List[MethodTranslation],
                               finishOps: List[Operation])

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
                              void: Boolean): Either[String, MethodTranslation] = {

    // We can't compute delta offset from generated asm code,
    // because in general asm generation requires stack offset,
    // which itself requires delta offsets
    def opcodeDeltaOffset(opcode: OpCode): Either[String, Int] = {
      def loadArg(num: Int): Int =
        if (!local && num == 0) 0 else 1

      val defltaOffset: PartialFunction[OpCode, Int] = {
        case LdcI40     => 1
        case LdcI41     => 1
        case LdcI42     => 1
        case LdcI43     => 1
        case LdcI44     => 1
        case LdcI45     => 1
        case LdcI46     => 1
        case LdcI47     => 1
        case LdcI48     => 1
        case LdcI4M1    => 1
        case LdcI4(num) => 1
        case LdcI4S(v)  => 1
        case LdcR4(f)   => 1
        case LdcR8(d)   => 1
        case Add        => -1
        case Mul        => -1
        case Div        => -1
        case Rem        => -1
        case Sub        => -1
        case Clt        => -1
        case Cgt        => -1
        case Ceq        => -1
        case Not        => 0

        case LdSFld(FieldData(_, _, _)) => 1
        case LdFld(FieldData(_, _, _))  => 1
        case StSFld(FieldData(_, _, _)) => -1
        case StFld(FieldData(_, _, _))  => -1

        case LdArg0      => loadArg(0)
        case LdArg1      => loadArg(1)
        case LdArg2      => loadArg(2)
        case LdArg3      => loadArg(3)
        case LdArg(num)  => loadArg(num)
        case LdArgS(num) => loadArg(num.toInt)

        case StLoc0      => -1
        case StLoc1      => -1
        case StLoc2      => -1
        case StLoc3      => -1
        case StLoc(num)  => -1
        case StLocS(num) => -1

        case LdLoc0      => 1
        case LdLoc1      => 1
        case LdLoc2      => 1
        case LdLoc3      => 1
        case LdLoc(num)  => 1
        case LdLocS(num) => 1

        case Nop          => 0
        case Ret          => 0
        case Jump(label)  => Constants.jumpStackOffset
        case JumpI(label) => Constants.jumpIStackOffset
        case Label(label) => Constants.labelStackOffset

        // FIXME consider to compute delta offset from generated asm
        case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
          val resO = for {
            parentSig <- signatures.get(parentSigIdx)
            methodSig <- signatures.get(methodSigIdx)
          } yield {
            lazy val defaultCall = {
              val paramsCnt = MethodTranslationUtils.methodParamsCount(methodSig)
              if (MethodTranslationUtils.isMethodVoid(methodSig)) -paramsCnt else -paramsCnt + 1
            }

            if (TypeTranslationUtils.detectMapping(parentSig)) {
              name match {
                case "get"        => -1
                case "getDefault" => -2
                case "exists"     => -1
                case "put"        => -3
                case _            => defaultCall
              }
            } else {
              defaultCall
            }
          }
          resO.getOrElse(0)
      }

      defltaOffset.lift(opcode).map(Right(_)).getOrElse(Left(s"Unknown opcode: $opcode"))
    }

    def translateOpcode(opcode: OpCode, stackOffsetO: Option[Int]): Either[String, List[Operation]] = {
      def pushTypedInt(i: Int): Operation =
        Operation.Push(Data.Primitive.Int32(i))

      def pushTypedFloat(d: Double): Operation =
        Operation.Push(Data.Primitive.Number(d))

      def computeLocalOffset(num: Int, stackOffset: Int): Int =
        (localsCount - num - 1) + stackOffset + 1

      def computeArgOffset(num: Int, stackOffset: Int): Int =
        (argsCount - num - 1) + stackOffset + localsCount + 1 + 1
      // for local there's additional object arg
      // for not local there's name of the method

      def storeLocal(num: Int): Either[String, List[Operation]] =
        stackOffsetO
          .map { s =>
            Right(
              List(
                Operation.Push(Data.Primitive.Int32(computeLocalOffset(num, s))),
                Operation(Opcodes.SWAPN),
                Operation(Opcodes.POP)
              ))
          }
          .getOrElse(Left("Stack offset is required for storing local variables"))

      def loadLocal(num: Int): Either[String, List[Operation]] =
        stackOffsetO
          .map { s =>
            Right(
              List(
                Operation.Push(Data.Primitive.Int32(computeLocalOffset(num, s))),
                Operation(Opcodes.DUPN)
              ))
          }
          .getOrElse(Left("Stack offset is required for loading local variables"))

      def loadArg(num: Int): Either[String, List[Operation]] =
        stackOffsetO
          .map { s =>
            if (local) {
              Right(
                List(
                  Operation.Push(Data.Primitive.Int32(computeArgOffset(num, s))),
                  Operation(Opcodes.DUPN)
                )
              )
            } else {
              if (num == 0) {
                Right(List.empty) // skip this reference
              } else {
                Right(
                  List(
                    Operation.Push(Data.Primitive.Int32(computeArgOffset(num - 1, s))),
                    Operation(Opcodes.DUPN)
                  )
                )
              }
            }
          }
          .getOrElse(Left("Stack offset is required for arguments loading"))

      def loadField(name: String, sigIdx: Long): List[Operation] = { // FIXME should process static fields too
        lazy val defaultLoad = List(
          Operation.Push(Data.Primitive.Utf8(name)),
          Operation(Opcodes.SGET)
        )

        signatures.get(sigIdx) match {
          case Some(FieldSig(tpe)) =>
            tpe match {
              case SigType.Generic(SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)), _) =>
                List(Operation.Push(Data.Primitive.Utf8(name)))
              case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) =>
                List(Operation.Push(Data.Primitive.Utf8(name)))
              case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) if name == "sender" =>
                List(Operation(Opcodes.FROM))
              case _ => defaultLoad
            }
          case _ => defaultLoad
        }
      }

      def storeField(name: String, sigIdx: Long): List[Operation] = { // FIXME should process static fields too
        lazy val defaultStore = List(
          Operation.Push(Data.Primitive.Utf8(name)),
          Operation(Opcodes.SPUT)
        )

        signatures.get(sigIdx) match {
          case Some(FieldSig(tpe)) =>
            tpe match {
              case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) =>
                List(Operation(Opcodes.STOP)) // error user shouldn't modify mappings
              case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) if name == "sender" =>
                List(Operation(Opcodes.STOP)) // error user shouldn't modify sender address
              case _ => defaultStore
            }
          case _ => defaultStore
        }
      }

      def callVirt(name: String, parentSigIdx: Long, methodSigIdx: Long): List[Operation] = {
        val resO = for {
          parentSig <- signatures.get(parentSigIdx)
          methodSig <- signatures.get(methodSigIdx)
        } yield {
          lazy val defaultCall = {
            val paramsCnt = MethodTranslationUtils.methodParamsCount(methodSig)
            if (MethodTranslationUtils.isMethodVoid(methodSig)) {
              List.fill(paramsCnt)(Operation(Opcodes.POP))
            } else {
              // TODO should throw error
              List.fill(paramsCnt)(Operation(Opcodes.POP)) :+ Operation.Push(Data.Primitive.Null)
            }
          }

          if (TypeTranslationUtils.detectMapping(parentSig)) {
            name match {
              case "get" => List(Operation(Opcodes.SWAP), Operation(Opcodes.CONCAT), Operation(Opcodes.SGET))
              case "getDefault" =>
                List(Operation.Call(Some("method_getDefault")))
              // FIXME we need some way to distinguish local functions and methods of program
              case "exists" => List(Operation(Opcodes.SWAP), Operation(Opcodes.CONCAT), Operation(Opcodes.SEXIST))
              case "put" =>
                List(
                  Operation.Push(Data.Primitive.Int32(2)),
                  Operation(Opcodes.DUPN),
                  Operation.Push(Data.Primitive.Int32(4)),
                  Operation(Opcodes.DUPN),
                  Operation(Opcodes.CONCAT),
                  Operation(Opcodes.SWAP),
                  Operation(Opcodes.SPUT),
                  Operation(Opcodes.POP),
                  Operation(Opcodes.POP)
                )
              case _ => defaultCall
            }
          } else {
            defaultCall
          }
        }

        resO.getOrElse(List.empty)
      }

      val translate: PartialFunction[OpCode, Either[String, List[Operation]]] = {
        case LdcI40     => Right(List(pushTypedInt(0)))
        case LdcI41     => Right(List(pushTypedInt(1)))
        case LdcI42     => Right(List(pushTypedInt(2)))
        case LdcI43     => Right(List(pushTypedInt(3)))
        case LdcI44     => Right(List(pushTypedInt(4)))
        case LdcI45     => Right(List(pushTypedInt(5)))
        case LdcI46     => Right(List(pushTypedInt(6)))
        case LdcI47     => Right(List(pushTypedInt(7)))
        case LdcI48     => Right(List(pushTypedInt(8)))
        case LdcI4M1    => Right(List(pushTypedInt(-1)))
        case LdcI4(num) => Right(List(pushTypedInt(num)))
        case LdcI4S(v)  => Right(List(pushTypedInt(v.toInt)))
        case LdcR4(f)   => Right(List(pushTypedFloat(f.toDouble)))
        case LdcR8(d)   => Right(List(pushTypedFloat(d)))
        case Add        => Right(List(Operation(Opcodes.ADD)))
        case Mul        => Right(List(Operation(Opcodes.MUL)))
        case Div        => Right(List(Operation(Opcodes.DIV)))
        case Rem        => Right(List(Operation(Opcodes.MOD)))
        case Sub        => Right(List(pushTypedInt(-1), Operation(Opcodes.MUL), Operation(Opcodes.ADD)))
        case Clt =>
          Right(
            List(
              Operation(Opcodes.LT),
              Operation.Push(Data.Primitive.Int8(Data.Type.Int8)), // cast to int
              Operation(Opcodes.CAST)
            ))
        case Cgt =>
          Right(
            List(
              Operation(Opcodes.LT),
              Operation.Push(Data.Primitive.Int8(Data.Type.Int8)), // cast to int
              Operation(Opcodes.CAST)
            ))
        case Ceq =>
          Right(
            List(
              Operation(Opcodes.LT),
              Operation.Push(Data.Primitive.Int8(Data.Type.Int8)), // cast to int
              Operation(Opcodes.CAST)
            ))
        case Not =>
          Right(
            List(
              Operation.Push(Data.Primitive.Int8(Data.Type.Boolean)), // cast to boolean
              Operation(Opcodes.CAST),
              Operation(Opcodes.NOT),
              Operation.Push(Data.Primitive.Int8(Data.Type.Int8)), // cast to int
              Operation(Opcodes.CAST)
            ))

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

        case Nop          => Right(List(Operation.Nop))
        case Ret          => Right(List())
        case Jump(label)  => Right(List(Operation.Jump(Some(label))))
        case JumpI(label) => Right(List(pushTypedInt(1), Operation(Opcodes.EQ), Operation.JumpI(Some(label))))
        case Label(label) => Right(List(Operation.Label(label)))

        case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
          Right(callVirt(name, parentSigIdx, methodSigIdx))
      } // FIXME partial function to either

      translate.lift(opcode).map(Right(_)).getOrElse(Left(s"Unknown opcode: $opcode")).joinRight
    }

    def transformStackOffset(Operation: OpCode,
                             labelOffsets: Map[String, Int],
                             stackOffsetO: Option[Int]): Either[String, (Map[String, Int], Option[Int])] = {
      val unstableStackError = Left("Unsupported sequence of instructions: stack is unstable")

      Operation match {
        case Label(label) =>
          (labelOffsets.get(label), stackOffsetO) match {
            case (Some(offset), Some(stackOffset)) if offset != stackOffset + Constants.labelStackOffset =>
              unstableStackError
            case (Some(offset), None)      => Right((labelOffsets, Some(offset)))
            case (None, Some(stackOffset)) => Right((labelOffsets.updated(label, stackOffset), Some(stackOffset)))
            case (_, offset @ _)           => Right((labelOffsets, offset))
          }
        case Jump(label) =>
          (labelOffsets.get(label), stackOffsetO) match {
            case (Some(offset), Some(stackOffset)) if offset != stackOffset + Constants.jumpStackOffset =>
              unstableStackError
            case (None, Some(stackOffset)) =>
              Right((labelOffsets.updated(label, stackOffset + Constants.jumpStackOffset), None))
            case (_, offset @ _) => Right((labelOffsets, None))
          }
        case JumpI(label) =>
          (labelOffsets.get(label), stackOffsetO) match {
            case (Some(offset), Some(stackOffset)) if offset != stackOffset + Constants.jumpIStackOffset =>
              unstableStackError
            case (None, Some(stackOffset)) =>
              Right((labelOffsets.updated(label, stackOffset + Constants.jumpIStackOffset), Some(stackOffset)))
            case (_, offset @ _) => Right((labelOffsets, offset))
          }
        case other => Right((labelOffsets, stackOffsetO))
      }
    }

    def traverseLabelOffsets(initLabelOffsets: Map[String, Int]): Either[String, Map[String, Int]] =
      opcodes
        .foldLeft[Either[String, (Map[String, Int], Option[Int])]](Right((initLabelOffsets, Some(0)))) {
          case (Right((labelOffsets, stackOffsetO)), op) =>
            for {
              so <- transformStackOffset(op, labelOffsets, stackOffsetO)
              (newLabelOffsets, newStackOffsetO) = so
              deltaOffset <- opcodeDeltaOffset(op)
            } yield (newLabelOffsets, newStackOffsetO.map(_ + deltaOffset))
          case (other, op) => other
        }
        .map(_._1)

    val convergeLabelOffsets: Either[String, Map[String, Int]] = {
      def go(labelOffsets: Map[String, Int]): Either[String, Map[String, Int]] = {
        for {
          newOffsets <- traverseLabelOffsets(labelOffsets)
          nextGo <- if (newOffsets != labelOffsets) go(newOffsets) else Right(newOffsets)
        } yield nextGo
      }

      go(Map.empty)
    }

    val opTranslationsE = {
      val res = for {
        convergedOffsets <- convergeLabelOffsets
      } yield
        opcodes
          .foldLeft[Either[String, (List[OpCodeTranslation], Option[Int])]](Right((List.empty, Some(0)))) {
            case (Right((acc, stackOffsetO)), op) =>
              for {
                so <- transformStackOffset(op, convergedOffsets, stackOffsetO)
                (_, newStackOffsetO) = so
                opcodes <- translateOpcode(op, newStackOffsetO)
                deltaOffset <- opcodeDeltaOffset(op)
              } yield (OpCodeTranslation(Right(op), stackOffsetO, opcodes) :: acc, newStackOffsetO.map(_ + deltaOffset))
            case (other, op) => other
          }
          .map(_._1.reverse)

      res.joinRight
    }

    val clear =
      if (void) {
        List.fill(localsCount + argsCount + 1)(Operation(Opcodes.POP))
      } else {
        List.fill(localsCount + argsCount + 1)(List(Operation(Opcodes.SWAP), Operation(Opcodes.POP))).flatten
      }

    for {
      opTranslations <- opTranslationsE
    } yield
      MethodTranslation(
        name,
        argsCount,
        localsCount,
        local,
        void,
        List(
          OpCodeTranslation(Left("method name"), None, List(Operation.Label("method_" + name))),
          OpCodeTranslation(Left("local vars"), None, List.fill(localsCount)(Operation.Push(Data.Primitive.Int32(0)))) // FIXME Should be replaced by proper value for local var type
        ) ++ opTranslations ++
          List(
            OpCodeTranslation(Left("local vars clearing"), None, clear),
            OpCodeTranslation(Left("end of a method"),
                              None,
                              if (local) List(Operation(Opcodes.RET)) else List(Operation.Jump(Some("stop"))))
          )
      )
  }

  def translateVerbose(rawMethods: List[Method],
                       cilData: CilData,
                       signatures: Map[Long, Signatures.Signature]): Either[String, Translation] = {
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

    val jumpToMethods = methods.flatMap {
      case (m, i) =>
        if (!isLocal(i)) {
          val name = cilData.tables.methodDefTable(i).name
          List(
            Operation(Opcodes.DUP),
            Operation.Push(Data.Primitive.Utf8(name)),
            Operation(Opcodes.EQ),
            Operation.JumpI(Some("method_" + name))
          )
        } else {
          List.empty
        }
    }

    val methodsOpsE: Either[String, List[MethodTranslation]] = methods.map {
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
              resolveBranches(m.opcodes),
              signatures,
              isLocal(i),
              isVoid
            )
        }
    }.sequence

    for {
      methodsOps <- methodsOpsE
    } yield Translation(jumpToMethods :+ Operation.Jump(Some("stop")), methodsOps, List(Operation.Label("stop")))
  }

  def translateAsm(rawMethods: List[Method],
                   cilData: CilData,
                   signatures: Map[Long, Signatures.Signature]): Either[String, List[Operation]] =
    for {
      t <- translateVerbose(rawMethods, cilData, signatures)
    } yield t.jumpToMethods ++ t.methods.flatMap(_.opcodes.flatMap(_.asm)) ++ t.finishOps
}
