package pravda.vm.state

import pravda.common.domain.{Address, NativeCoin}

trait Environment {
  // Programs
  def updateProgram(address: Address, code: Data): Data
  def createProgram(owner: Address, code: Data): Address
  def getProgram(address: Address): Option[ProgramContext]
  def getProgramOwner(address: Address): Option[Address]

  // Balance
  def transfer(from: Address, to: Address, amount: NativeCoin): Unit
  def balance(address: Address): NativeCoin

  def withdraw(address: Address, amount: NativeCoin): Unit
  def accrue(address: Address, amount: NativeCoin): Unit

}
