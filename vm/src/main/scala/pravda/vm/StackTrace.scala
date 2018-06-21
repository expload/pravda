package pravda.vm

import pravda.common.domain.Address
import pravda.vm.StackTrace.Point

final case class StackTrace(stackTrace: Seq[Point]) {

  def +(p: Point): StackTrace = {
    copy(stackTrace = p +: stackTrace)
  }
}

object StackTrace {

  final case class Point(callStack: Seq[Int], position: Int, address: Option[Address])

  def apply(p: Point): StackTrace = {
    StackTrace(List(p))
  }

  def empty: StackTrace = {
    StackTrace(List.empty[Point])
  }
}
