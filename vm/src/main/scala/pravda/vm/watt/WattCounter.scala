package pravda.vm.watt

import pravda.vm.state.{Data, Memory}
import pravda.vm.state.VmError.OutOfGas


class WattCounter(wattLimit: Long) {
  val StorageOccupyFactor = 4
  val StorageReleaseFactor = 2

  var cpuWatts = 0L
  var storageWatts = 0L
  var memoryWatts = 0L

  var extraWatts = 0L

  var storageRefund = 0L
  var extraRefund = 0L

  def refund: Long = storageRefund + extraRefund

  def spent: Long = extraWatts + cpuWatts + memoryWatts + storageWatts

  def storageUsage(occupiedBytes: Long = 0L, releasedBytes: Long = 0L): Unit = {
        storageWatts += occupiedBytes * StorageOccupyFactor
        storageRefund += releasedBytes * StorageReleaseFactor
        check()
  }

  def memoryUsage(memoryBytesInUse: Long): Unit = {
    memoryWatts = memoryBytesInUse * memoryBytesInUse // TODO better function
  }

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

  def total: Long = {

    val spentWatts = spent
    if(spentWatts > wattLimit) wattLimit
    else spentWatts - Math.min(refund, spentWatts / 2)

  }

  def limit: Long = wattLimit

}

object WattCounter {

  val CpuBasic = 1
  val CpuSimpleArithmetic = 5
  val CpuArithmetic = 10

  val CpuProgControl = 5
  val CpuExtCall = 10
  val CpuStorageUse = 20

  private val CpuWordDelimiter = 64
  def CpuWordOperation(word: Data*): Int = word.map(_.size()).sum / CpuWordDelimiter

}
