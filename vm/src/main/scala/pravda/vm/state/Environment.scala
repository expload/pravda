package pravda.vm.state

trait Environment {
  // Programs
  def updateProgram(address: Data, code: Data): Unit
  def createProgram(owner: Address, code: Data): Address
  def getProgram(address: Address): Option[ProgramContext]
  def getProgramOwner(address: Address): Option[Address]
}
