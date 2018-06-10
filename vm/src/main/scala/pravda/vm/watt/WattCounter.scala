package pravda.vm.watt

import pravda.vm.state.{Data, VmErrorException}
import pravda.vm.state.VmError.OutOfWatts

class WattCounter(wattLimit: Long) {

  val StorageOccupyFactor = 8L
  val StorageReleaseFactor = 2L
  val CpuFactor = 1L

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
    val M100 = 100000000L
    def cube(v: Long) = v * v * v
    memoryWatts = 100 * cube(memoryBytesInUse / M100 + 1) // TODO better function
    check()
  }

  def cpuUsage(timeUnits: Long*): Unit = {
    cpuWatts += timeUnits.sum * CpuFactor
    check()
  }

  def extra(watts: Long): Unit = {
    extraWatts += watts
    check()
  }

  def refund(watts: Long): Unit = {
    extraRefund += watts
  }

  def check(): Unit = {
    if (spent > wattLimit) throw VmErrorException(OutOfWatts)
  }

  def total: Long = {

    val spentWatts = spent
    if (spentWatts > wattLimit) wattLimit
    else spentWatts - Math.min(refund, spentWatts / 2)

  }

  def limit: Long = wattLimit

}

object WattCounter {

  val CpuBasic = 1L
  val CpuSimpleArithmetic = 5L
  val CpuArithmetic = 10L

  val CpuProgControl = 5L
  val CpuExtCall = 10L
  val CpuStorageUse = 20L

  private val CpuWordDelimiter = 64L

  def CpuWordOperation(word: Data*): Long = word.map(_.volume).sum / CpuWordDelimiter
}
