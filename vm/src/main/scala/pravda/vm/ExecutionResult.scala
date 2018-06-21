package pravda.vm

final case class ExecutionResult(memory: Memory, error: Option[VmErrorException], wattCounter: WattCounter) {
  def isSuccess: Boolean = error.isEmpty
}
