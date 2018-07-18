package pravda.vm.operations

import pravda.vm.VmError.OperationDenied
import pravda.vm.WattCounter._
import pravda.vm.Data.Primitive
import pravda.vm._
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.Opcodes._

/**
  * Pravda VM storage pravda.vm.Opcodes implementation.
  * @see pravda.vm.Opcodes
  * @param memory Access to VM memory
  * @param maybeStorage Access to program's storage
  */
final class StorageOperations(memory: Memory, maybeStorage: Option[Storage], wattCounter: WattCounter) {

  @OpcodeImplementation(
    opcode = SEXIST,
    description =
      "Pops first item from stack, interprets it as key and checks existence of record correspond to the key in a storage of the program. "
  )
  def exists(): Unit = ifStorage { storage =>
    val key = memory.pop()
    val defined = storage.get(key).isDefined
    val data = Primitive.Bool(defined)
    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

  @OpcodeImplementation(
    opcode = SDROP,
    description =
      "Pops first item from stack, interprets it as key and removes corresponding record from a storage of the program. "
  )
  def drop(): Unit = ifStorage { storage =>
    // TODO fomkin: consider to push removed value to the stack
    val key = memory.pop()
    val maybePrevious = storage.delete(key)

    wattCounter.cpuUsage(CpuStorageUse)
    maybeReleaseStorage(key.volume, maybePrevious)
  }

  @OpcodeImplementation(
    opcode = SGET,
    description =
      "Pops first item from stack, interprets it as key, retrieves corresponding record from a storage of the program and pushes the record to the stack. Otherwise throws an exception. "
  )
  def get(): Unit = ifStorage { storage =>
    val data = storage
      .get(memory.pop())
      .collect { case p: Data.Primitive => p }
      .getOrElse(Data.Primitive.Null)
    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

  @OpcodeImplementation(
    opcode = SPUT,
    description =
      "Pops first item from stack, interprets it as key. Pops second item from stack, interprets it as value. Puts (key -> value) record to program's storage. "
  )
  def put(): Unit = ifStorage { storage =>
    val value = memory.pop()
    val key = memory.pop()
    val bytesTotal = value.volume + key.volume
    val maybePrevious = storage.put(key, value)

    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.storageUsage(occupiedBytes = bytesTotal.toLong)
    maybeReleaseStorage(key.volume, maybePrevious)
  }

  private def ifStorage(f: Storage => Unit): Unit = maybeStorage match {
    case None          => throw VmErrorException(OperationDenied)
    case Some(storage) => f(storage)
  }

  private def maybeReleaseStorage(keySize: Int, maybePrevious: Option[Data]): Unit = {
    maybePrevious foreach { previous =>
      val releasedBytesTotal = keySize.toLong + previous.volume.toLong
      wattCounter.storageUsage(releasedBytes = releasedBytesTotal)
    }
  }
}
