package pravda.vm.watt

import pravda.vm.state.{Data, Memory}
import pravda.vm.state.VmError.OutOfGas


class WattCounter(wattLimit: Long, memory: Memory) {

  var cpuWatts = 0L
  var extraWatts = 0L

  var refund = 0L

  def memoryWatts: Long = memory.maxVolume * memory.maxVolume // TODO proper function

  def storageWatts: Long = 0L

  def spent: Long = extraWatts + cpuWatts + memoryWatts + storageWatts

  def cpuUsage(watts: Long*): Unit = {
    cpuWatts += watts.sum
    check()
  }

  def extra(watts: Long): Unit = {
    extraWatts += watts
    check()
  }

  def refund(watts: Long): Unit = {
    refund += watts
  }

  def check(): Unit = {
    if (spent > wattLimit) throw OutOfGas()
  }

  def total(): Long = {
    val spentWatts = spent
    spentWatts - Math.min(refund, spentWatts / 2)
  }

}

object WattCounter {

  val CPUBasic = 1
  val CPUSimpleArithmetic = 5
  val CPUArithmetic = 10

  val CPUProgControl = 5
  val CPUExtCall = 10
  val CPUStorageUse = 100

  private val CPUWordFactor = 1
  def CPUWordOperation(word: Data*): Int = word.map(_.size()).sum * CPUWordFactor

}
