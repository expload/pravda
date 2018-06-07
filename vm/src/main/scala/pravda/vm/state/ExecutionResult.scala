package pravda.vm.state

import pravda.vm.watt.WattCounter

final case class ExecutionResult(
    memory: Memory,
    error: Option[VmErrorException],
    wattCounter: WattCounter
) {

  import ExecutionResult._

  def status: Status = error.fold[Status](Ok)(_ => Error)

}


object ExecutionResult {

  trait Status

  case object Ok extends Status
  case object Error extends Status
}