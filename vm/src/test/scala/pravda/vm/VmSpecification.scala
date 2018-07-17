//package pravda.vm
//
//import cats.data.State
//import com.google.protobuf.ByteString
//import com.softwaremill.quicklens._
//import org.scalacheck.Properties
//import pravda.vm.Data.Primitive.{Bool, Int32}
//import supertagged.TaggedType
//
///**
//  * This specification expressed as set of rules which are
//  * can be applied for execution of bytecode
//  * ([[pravda.vm.asm.Operation]] in terms of assembler) for
//  * test programs generation. Test programs have no sence,
//  * but they have predictable result, beause the specification
//  * can run generated code by itself.
//  */
//object VmSpecification extends Properties("Vm") {
//
////  private val rules: Int => Machine = (program, state) => {
////    case Opcodes.STOP => state.copy(stopped = true)
////    case Opcodes.JUMP =>
////
////    case Opcodes.PUSHX =>
////      Data.fromByteString(program, state.position) match {
////        case (position, data: Data.Primitive) =>
////          state.push(data).copy(position = Offset @@ position)
////        case _ => state.copy(error = Some(VmError.WrongType))
////      }
////    case Opcodes.POP =>
////      state.pop match {
////        case (newState, Some(_)) => newState
////        case (newState, None) =>
////          newState.copy(error = Some(VmError.StackUnderflow))
////      }
////    case Operation.Orphan(Opcodes.RET, _) =>
////      state.callStack match {
////        case Nil => state.copy(error = Some(VmError.CallStackUnderflow))
////        case x :: xs => state.copy(callStack = xs, position = x)
////      }
////  }
//
//  // -------------
//
//  object Program extends TaggedType[ByteString]
//  type Program = Program.Type
//
//  object Offset extends TaggedType[Int]
//  type Offset = Offset.Type
//
//  object ModuleId extends TaggedType[String]
//  type ModuleId = ModuleId.Type
//
//  case class Machine(program: Program,
//                     heap: Map[Int, Data],
//                     stack: List[Data.Primitive],
//                     callStack: List[Offset],
//                     position: Offset,
//                     error: Option[VmError],
//                     stopped: Boolean)
//
//  val nextOpcode: State[Machine, Int] =
//    State(machine => (machine.modify(_.position).using(x => Offset @@ (x + 1)), machine.position))
//
//  val readDataFromProgram: State[Machine, Data] = State { machine =>
//    val (position, data) = Data.fromByteString(machine.program, machine.position)
//    (machine.modify(_.position).setTo(Offset @@ position), data)
//  }
//
//  val position: State[Machine, Offset] =
//    State.inspect(_.position)
//
//  val pop: State[Machine, Option[Data.Primitive]] =
//    State(machine => (machine.modify(_.stack).using(_.tail), machine.stack.headOption))
//
//  val doNothing: State[Machine, Unit] =
//    State.pure(())
//
//  val stop: State[Machine, Unit] =
//    State.modify(_.copy(stopped = true))
//
//  def jump(position: Offset): State[Machine, Unit] =
//    State.modify(_.copy(position = position))
//
//  def push(data: Data.Primitive): State[Machine, Unit] =
//    State.modify(_.modify(_.stack).using(data :: _))
//
//  def error(error: VmError): State[Machine, Unit] =
//    State.modify(_.copy(error = Some(error)))
//
//  val run: State[Machine, Unit] = nextOpcode flatMap {
//    case Opcodes.STOP =>
//      stop
//    case Opcodes.JUMP =>
//      pop.flatMap {
//        case Some(Int32(data)) => jump(Offset @@ data)
//        case Some(_) => error(VmError.WrongType)
//        case None => error(VmError.StackUnderflow)
//      }
//    case Opcodes.JUMPI =>
//      for {
//        offset <- pop
//        condition <- pop
//        _ <- (offset, condition) match {
//          case (Some(Int32(data)), Some(Bool.True)) => jump(Offset @@ data)
//          case (Some(Int32(_)), Some(Bool.False)) => doNothing
//          case (Some(_), Some(_)) => error(VmError.WrongType)
//          case (None, _) => error(VmError.StackUnderflow)
//        }
//      } yield ()
//    case Opcodes.CALL =>
//      for {
//        current <- position
//        target <- pop
//        _ <- State.modify[Machine](_.modify(_.callStack).using(current :: _))
//        _ <- target match {
//          case Some(Int32(x)) => jump(Offset @@ x)
//          case None => error(VmError.StackUnderflow)
//        }
//      } yield ()
//    case Opcodes.RET =>
//      for {
//        offset <- State[Machine, Option[Offset]] { machine =>
//          (machine.modify(_.callStack).using(_.tail), machine.callStack.headOption)
//        }
//        _ <- offset match {
//          case Some(x) => jump(x)
//          case None => error(VmError.CallStackUnderflow)
//        }
//      } yield ()
//    case Opcodes.PCALL => doNothing // TODO
//    case Opcodes.LCALL => doNothing // TODO
//
//  }
//}
