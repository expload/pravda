package pravda.vm.watt

import pravda.vm.state.{Data, Storage}

class StorageWithCounter(storage: Storage, wattCounter: WattCounter) extends Storage {
  import WattCounter._

  override def get(key: Data): Option[Data] = {
    wattCounter.cpuUsage(CpuStorageUse)

    storage.get(key)
  }

  override def put(key: Data, value: Data): Option[Data] = {
    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.storageUsage(occupiedBytes = value.size().toLong + key.size().toLong)

    val prev = storage.put(key, value)
    prev.foreach { d =>
      wattCounter.storageUsage(releasedBytes = d.size().toLong + key.size().toLong)
    }
    prev
  }

  override def delete(key: Data): Option[Data] = {
    wattCounter.cpuUsage(CpuStorageUse)

    val prev = storage.delete(key)
    prev.foreach { d =>
      wattCounter.storageUsage(releasedBytes = d.size().toLong + key.size().toLong)
    }
    prev
  }

}
