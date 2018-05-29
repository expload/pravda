package pravda.vm.state

import pravda.common.domain.Address

trait Environment {
  // Programs
  def updateProgram(address: Address, code: Data): Unit
  def createProgram(owner: Address, code: Data): Address
  def getProgram(address: Address): Option[ProgramContext]
  def getProgramOwner(address: Address): Option[Address]
}
