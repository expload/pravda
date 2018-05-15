package io.mytc.sood.vm.state

trait Environment {
  def getProgram(address: Address): Option[Program]
}
