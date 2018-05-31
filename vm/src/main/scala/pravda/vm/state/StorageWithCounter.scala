package pravda.vm.state

import pravda.vm.watt.WattCounter

class StorageWithCounter(storage: Storage, wattCounter: WattCounter) extends  Storage {
  import WattCounter._

  override def get(key: Data): Option[Data] = {
    wattCounter.cpuUsage(CPUStorageUse)

    storage.get(key)
  }

  override def put(key: Data, value: Data): Option[Data] = {
    wattCounter.cpuUsage(CPUStorageUse)
    wattCounter.storageUsage(occupiedBytes = value.size() + key.size())

    val prev = storage.put(key, value)
    prev.foreach{ d =>
      wattCounter.storageUsage(releasedBytes = d.size() + key.size())
    }
    prev
  }

  override def delete(key: Data): Option[Data] = {
    wattCounter.cpuUsage(CPUStorageUse)

    val prev = storage.delete(key)
    prev.foreach{ d =>
      wattCounter.storageUsage(releasedBytes = d.size() + key.size())
    }
    prev
  }

}
