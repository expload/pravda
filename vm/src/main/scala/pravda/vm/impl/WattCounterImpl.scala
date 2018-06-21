package pravda.vm.impl

import pravda.vm.VmError.OutOfWatts
import pravda.vm.{VmErrorException, WattCounter}

final class WattCounterImpl(val limit: Long) extends WattCounter {

  import WattCounter._

  private var cpuWatts = 0L
  private var storageWatts = 0L
  private var memoryWatts = 0L
  private var extraWatts = 0L
  private var storageRefund = 0L
  private var extraRefund = 0L

  def refund: Long =
    storageRefund + extraRefund

  def spent: Long =
    extraWatts + cpuWatts + memoryWatts + storageWatts

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
    if (spent > limit) throw VmErrorException(OutOfWatts)
  }

  def total: Long = {
    val spentWatts = spent
    if (spentWatts > limit) limit
    else spentWatts - Math.min(refund, spentWatts / 2)
  }
}
