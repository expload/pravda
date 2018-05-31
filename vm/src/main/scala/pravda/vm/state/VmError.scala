package pravda.vm.state

import pravda.common.domain.Address

sealed trait VmError {
  val code: Int
}

object VmError {

  case object StackOverflow   extends VmError { val code = 100 }
  case object StackUnderflow  extends VmError { val code = 101 }
  case object WrongStackIndex extends VmError { val code = 102 }
  case object WrongHeapIndex  extends VmError { val code = 103 }
  case object OperationDenied extends VmError { val code = 200 }
  case object NoSuchProgram   extends VmError { val code = 300 }
  case object NoSuchLibrary   extends VmError { val code = 301 }
  case object NoSuchMethod    extends VmError { val code = 302 }
  case object NoSuchElement   extends VmError { val code = 400 }
  case object OutOfGas        extends VmError { val code = 500 }

  final case class SomethingWrong(ex: Exception) extends VmError { val code = 999 }

}

final case class VmErrorException(error: VmError, stackTrace: StackTrace = StackTrace.empty) extends Exception {
  def addToTrace(p: Point): VmErrorException = copy(stackTrace = stackTrace + p)
}

final case class StackTrace(stackTrace: Seq[Point]) {

  def +(p: Point): StackTrace = {
    copy(stackTrace = p +: stackTrace)
  }
}

object StackTrace {

  def apply(p: Point): StackTrace = {
    StackTrace(List(p))
  }

  def empty: StackTrace = {
    StackTrace(List.empty[Point])
  }
}

final case class Point(callStack: Seq[Int], position: Int, address: Option[Address])
