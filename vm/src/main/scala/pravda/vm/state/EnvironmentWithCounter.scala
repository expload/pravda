package pravda.vm.state

import pravda.common.domain.{Address, NativeCoins}
import pravda.vm.watt.WattCounter
import pravda.vm.watt.WattCounter.CPUStorageUse

class EnvironmentWithCounter(environment: Environment, wattCounter: WattCounter) extends Environment {

  def updateProgram(address: Address, code: Data): Data = {
    wattCounter.cpuUsage(CPUStorageUse)
    wattCounter.storageUsage(occupiedBytes = code.size())

    val previous = environment.updateProgram(address, code)
    wattCounter.storageUsage(releasedBytes = previous.size())
    previous
  }

  def createProgram(owner: Address, code: Data): Address = {
    wattCounter.cpuUsage(CPUStorageUse)
    wattCounter.storageUsage(occupiedBytes = code.size())

    environment.createProgram(owner, code)
  }

  def getProgram(address: Address): Option[ProgramContext] = {
    wattCounter.cpuUsage(CPUStorageUse)

    environment.getProgram(address)
  }

  def getProgramOwner(address: Address): Option[Address] = {
    wattCounter.cpuUsage(CPUStorageUse)

    environment.getProgramOwner(address)
  }

  def transfer(from: Address, to: Address, amount: NativeCoins): Unit = {
    wattCounter.cpuUsage(CPUStorageUse)

    environment.transfer(from, to, amount)
  }

  def balance(address: Address): NativeCoins = {
    wattCounter.cpuUsage(CPUStorageUse)

    environment.balance(address)
  }

  def withdraw(address: Address, amount: NativeCoins): Unit = {
    wattCounter.cpuUsage(CPUStorageUse)

    environment.withdraw(address, amount)
  }

  def put(address: Address, amount: NativeCoins): Unit = {
    wattCounter.cpuUsage(CPUStorageUse)

    environment.put(address, amount)
  }

}
