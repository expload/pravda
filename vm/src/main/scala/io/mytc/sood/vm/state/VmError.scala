package io.mytc.sood.vm.state

sealed trait VmError {
  val code: Int
}

object VmError {

  case object StackOverflow extends VmError { val code = 100 }
  case object StackUnderflow extends VmError { val code = 101 }
  case object OperationDenied extends VmError { val code = 200 }
  case object NoSuchProgram extends VmError { val code = 300 }
  case object NoSuchLibrary extends VmError { val code = 301 }
  case object NoSuchMethod extends VmError { val code = 302 }

  case class SomethingWrong(ex: Exception) extends VmError {val code = 999 }

}

case class VmErrorException(error: VmError, stackTrace: StackTrace = StackTrace.empty) extends Exception {
  def addToTrace(p: Point): VmErrorException = copy(stackTrace = stackTrace + p)
}

case class StackTrace(stackTrace: Seq[Point]) {
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

case class Point(callStack: Seq[Int], position: Int, address: Option[Address])
