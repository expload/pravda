package pravda.vm.watt

import pravda.vm.state.{Data, Storage}

class StorageWithCounter(storage: Storage, wattCounter: WattCounter) extends Storage {

  import WattCounter._

  def get(key: Data): Option[Data] = {
    wattCounter.cpuUsage(CpuStorageUse)
    storage.get(key)
  }

  def put(key: Data, value: Data): Option[Data] = {
    val keyBytes = key.toByteString
    val valueBytes = value.toByteString
    val bytesTotal = valueBytes.size().toLong + keyBytes.size().toLong
    val maybePrevious = storage.put(Data.MarshalledData(keyBytes), Data.MarshalledData(valueBytes))

    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.storageUsage(occupiedBytes = bytesTotal)
    maybeReleaseStorage(keyBytes.size(), maybePrevious)
  }

  def delete(key: Data): Option[Data] = {
    val keyBytes = key.toByteString
    val maybePrevious = storage.delete(Data.MarshalledData(keyBytes))
    wattCounter.cpuUsage(CpuStorageUse)
    maybeReleaseStorage(keyBytes.size(), maybePrevious)
  }

  private def maybeReleaseStorage(keySize: Int, maybePrevious: Option[Data]) = {
    maybePrevious foreach { previous =>
      val releasedBytesTotal = keySize.toLong + previous.volume.toLong
      wattCounter.storageUsage(releasedBytes = releasedBytesTotal)
    }
    maybePrevious
  }
}
