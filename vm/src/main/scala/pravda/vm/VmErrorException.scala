package pravda.vm

import pravda.vm.StackTrace.Point

final case class VmErrorException(error: VmError, stackTrace: StackTrace = StackTrace.empty) extends Exception {
  def addToTrace(p: Point): VmErrorException = copy(stackTrace = stackTrace + p)
}
