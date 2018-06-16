package pravda.vm.state

import pravda.common.domain.Address

sealed abstract class VmError(val code: Int)

object VmError {

  case object StackOverflow     extends VmError(100)
  case object StackUnderflow    extends VmError(101)
  case object WrongStackIndex   extends VmError(102)
  case object WrongHeapIndex    extends VmError(103)
  case object WrongType         extends VmError(104)
  case object InvalidCoinAmount extends VmError(104)
  case object InvalidAddress    extends VmError(104)

  case object OperationDenied      extends VmError(200)
  case object NoSuchProgram        extends VmError(300)
  case object NoSuchLibrary        extends VmError(301)
  case object NoSuchMethod         extends VmError(302)
  case object NoSuchElement        extends VmError(400)
  case object OutOfWatts           extends VmError(500)
  case object CallStackOverflow    extends VmError(600)
  case object ExtCallStackOverflow extends VmError(601)

  final case class SomethingWrong(ex: Throwable) extends VmError(999)

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
