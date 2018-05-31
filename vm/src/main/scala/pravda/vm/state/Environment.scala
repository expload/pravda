package pravda.vm.state

import pravda.common.domain.{Address, NativeCoins}

trait Environment {
  // Programs
  def updateProgram(address: Address, code: Data): Unit
  def createProgram(owner: Address, code: Data): Address
  def getProgram(address: Address): Option[ProgramContext]
  def getProgramOwner(address: Address): Option[Address]

  // Balance
  trait BalanceError
  case object NotEnoughMoney extends BalanceError

  def transfer(from: Address, to: Address, amount: NativeCoins): Either[BalanceError, Unit]
  def balance(address: Address): NativeCoins

  def withdraw(address: Address, amount: NativeCoins): Either[BalanceError, Unit]
  def put(address: Address, amount: NativeCoins): Unit

}
