package io.mytc.sood.vm.state

sealed trait Error {
  val code: Int
}

object Error {

  case object StackOverflow extends Error { val code = 100 }
  case object StackUnderflow extends Error { val code = 101 }
  case object OperationDenied extends Error { val code = 200 }
  case object NoSuchProgram extends Error { val code = 300 }
  case object NoSuchLibrary extends Error { val code = 301 }
  case object NoSuchMethod extends Error { val code = 302 }
  case object ExtraReturn extends Error { val code = 400 }

  case class SomethingWrong(ex: Exception) extends Error {val code = 999 }

}

case class VmError(error: Error, stackTrace: StackTrace = StackTrace.empty) extends Exception {
  def appendToTrace(p: Point): VmError = copy(stackTrace = stackTrace + p)
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
