package pravda.vm

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}

trait Environment {

  /**
    * Current executor
    */
  def executor: Address

  // Programs
  def updateProgram(address: Address, code: ByteString): Unit
  def createProgram(owner: Address, code: ByteString): Address
  def getProgram(address: Address): Option[ProgramContext]
  def getProgramOwner(address: Address): Option[Address]

  // Balance
  def balance(address: Address): NativeCoin
  def transfer(from: Address, to: Address, amount: NativeCoin): Unit
  def accrue(address: Address, amount: NativeCoin): Unit
  def withdraw(address: Address, amount: NativeCoin): Unit
}
