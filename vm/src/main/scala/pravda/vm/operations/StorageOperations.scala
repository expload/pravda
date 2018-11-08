/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.vm.operations

import pravda.vm.Error.OperationDenied
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
      "Pops first item from stack, interprets it as key, retrieves corresponding" +
        " record from a storage of the program and pushes the record to the " +
        "stack. Otherwise throws an exception. "
  )
  def get(): Unit = ifStorage { storage =>
    wattCounter.cpuUsage(CpuStorageUse)
    storage.get(memory.pop()) match {
      case None =>
        wattCounter.memoryUsage(1l)
        memory.push(Data.Primitive.Null)
      case Some(p: Data.Primitive) =>
        wattCounter.memoryUsage(p.volume.toLong)
        memory.push(p)
      case Some(s) =>
        wattCounter.memoryUsage(s.volume.toLong)
        memory.push(memory.heapPut(s))
    }
  }

  @OpcodeImplementation(
    opcode = SPUT,
    description =
      "Pops first item from stack, interprets it as key. Pops second item from stack, " +
        "interprets it as value. Puts (key -> value) record to program's storage. " +
        "If value is a ref, correspondent value will be taken from heap. " +
        "Referenced value shouldn't be RefArray and shouldn't be Struct with refs in " +
        "field values."
  )
  def put(): Unit = ifStorage { storage =>
    val key = memory.pop()
    val value = memory.pop() match {
      case ref:Data.Primitive.Ref =>
        memory.heapGet(ref) match {
          case _: Data.Array.RefArray =>
            throw ThrowableVmError(Error.WrongType)
          case _: Data.Primitive.Ref =>
            throw ThrowableVmError(Error.WrongType)
          case _: Data.Primitive.Offset =>
            throw ThrowableVmError(Error.WrongType)
          case struct: Data.Struct =>
            // Flat object
            val isFlat = struct.data.forall {
              case (_, _: Data.Primitive.Ref) => false
              case (_, _: Data.Primitive.Offset) => false
              case _ => true
            }
            if (isFlat) struct
            else throw ThrowableVmError(Error.WrongType)
          case x => x
        }
      case x:Data.Primitive => x
      case _ => throw ThrowableVmError(Error.WrongType)
    }
    val bytesTotal = value.volume + key.volume
    val maybePrevious = storage.put(key, value)

    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.storageUsage(occupiedBytes = bytesTotal.toLong)
    maybeReleaseStorage(key.volume, maybePrevious)
  }

  private def ifStorage(f: Storage => Unit): Unit = maybeStorage match {
    case None          => throw ThrowableVmError(OperationDenied)
    case Some(storage) => f(storage)
  }

  private def maybeReleaseStorage(keySize: Int, maybePrevious: Option[Data]): Unit = {
    maybePrevious foreach { previous =>
      val releasedBytesTotal = keySize.toLong + previous.volume.toLong
      wattCounter.storageUsage(releasedBytes = releasedBytesTotal)
    }
  }
}
