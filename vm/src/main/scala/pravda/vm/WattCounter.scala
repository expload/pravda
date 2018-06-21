package pravda.vm

trait WattCounter {

  def refund: Long

  def spent: Long

  def total: Long

  def limit: Long

  def storageUsage(occupiedBytes: Long = 0L, releasedBytes: Long = 0L)

  def memoryUsage(memoryBytesInUse: Long): Unit

  def cpuUsage(timeUnits: Long*): Unit

  def extra(watts: Long): Unit

  def refund(watts: Long): Unit

  def check(): Unit
}

object WattCounter {

  val StorageOccupyFactor = 8L
  val StorageReleaseFactor = 2L
  val CpuFactor = 1L

  val CpuBasic = 1L
  val CpuSimpleArithmetic = 5L
  val CpuArithmetic = 10L

  val CpuProgControl = 5L
  val CpuExtCall = 10L
  val CpuStorageUse = 20L

  private val CpuWordDelimiter = 64L

  def CpuWordOperation(word: Data*): Long =
    word.map(_.volume).sum / CpuWordDelimiter
}
