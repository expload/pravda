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

package pravda.vm.sandbox

import com.google.protobuf.ByteString
import pravda.common.bytes
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm.Error.DataError
import pravda.vm._
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object VmSandbox {

  class EnvironmentSandbox(effects: mutable.Buffer[vm.Effect],
                           initStorages: Map[Address, Map[Primitive, Data]],
                           initBalances: Seq[(Address, Primitive.Int64)],
                           initPrograms: Seq[(Address, Primitive.Bytes)],
                           pExecutor: Address,
                           appStateInfo: AppStateInfo)
      extends Environment {

    private val balances = mutable.Map(initBalances: _*)
    private val programs = mutable.Map(initPrograms: _*)
    private val sealedPrograms = mutable.Map[Address, Boolean]()

    def executor: Address = pExecutor

    def sealProgram(address: Address): Unit = {
      sealedPrograms(address) = true
      effects += vm.Effect.ProgramSeal(address)
    }

    def updateProgram(address: Address, code: ByteString): Unit = {
      programs(address) = Data.Primitive.Bytes(code)
      effects += vm.Effect.ProgramUpdate(address, Data.Primitive.Bytes(code))
    }

    def createProgram(address: Address, code: ByteString): Unit = {
      programs(address) = Data.Primitive.Bytes(code)
      effects += vm.Effect.ProgramCreate(address, Data.Primitive.Bytes(code))
    }

    def getProgram(address: Address): Option[ProgramContext] = {
      programs.get(address).map { p =>
        ProgramContext(
          new StorageSandbox(address, effects, initStorages.getOrElse(address, Map.empty).toSeq),
          p.data,
          `sealed` = sealedPrograms.getOrElse(address, false)
        )
      }
    }

    def balance(address: Address): NativeCoin = {
      val balance = NativeCoin @@ balances.get(address).fold(0L)(_.data)
      effects += vm.Effect.ShowBalance(address, balance)
      balance
    }

    def transfer(from: Address, to: Address, amount: NativeCoin): Unit = {
      val fromBalance = balances.get(from).fold(0L)(_.data)
      val toBalance = balances.get(to).fold(0L)(_.data)
      balances(from) = Data.Primitive.Int64(fromBalance - amount)
      balances(to) = Data.Primitive.Int64(toBalance + amount)
      effects += vm.Effect.Transfer(from, to, amount)
    }

    def event(address: Address, name: String, data: MarshalledData): Unit =
      effects += vm.Effect.Event(address, name, data)

    def chainHeight = appStateInfo.height
    def lastBlockHash = appStateInfo.`app-hash`
    def lastBlockTime = appStateInfo.timestamp
  }

  class StorageSandbox(address: Address, effects: mutable.Buffer[vm.Effect], initStorage: Seq[(Data.Primitive, Data)])
      extends Storage {

    val storageItems: mutable.Map[Data.Primitive, Data] = mutable.Map(initStorage: _*)

    def get(key: Data.Primitive): Option[Data] = {
      val value = storageItems.get(key)
      effects += vm.Effect.StorageRead(address, key, value)
      value
    }

    def put(key: Data.Primitive, value: Data): Option[Data] = {
      val prev = storageItems.put(key, value)
      effects += vm.Effect.StorageWrite(address, key, prev, value)
      prev
    }

    def delete(key: Data.Primitive): Option[Data] = {
      val prev = storageItems.remove(key)
      effects += vm.Effect.StorageRemove(address, key, prev)
      prev
    }
  }

  /**
    * Preconditions for VM sandbox
    *
    * @param balances Balances of accounts
    * @param stack Initial stack of VM
    * @param heap Initial heap of VM
    * @param storage Initial storage, the program is run on the account with zero address (0x00 32 times)
    * @param `program-storage` Initial storages for given account addresses
    * @param programs Bytecodes of programs with given addresses
    * @param executor Address of executor of the program, default executor address is 0x010203...1E1F20
    * @param code Bytecode of the program
    * @param `app-state-info` Various Blockchain info
    */
  final case class Preconditions(
      `watts-limit`: Long = 1000000L,
      balances: Map[Address, Primitive.Int64] = Map.empty,
      stack: Seq[Primitive] = Nil,
      heap: Map[Primitive.Ref, Data] = Map.empty,
      storage: Map[Primitive, Data] = Map.empty,
      `program-storage`: Map[Address, Map[Primitive, Data]] = Map.empty,
      programs: Map[Address, Primitive.Bytes] = Map.empty,
      executor: Option[Address] = None,
      `app-state-info`: AppStateInfo = AppStateInfo(
        `app-hash` = bytes.hex2byteString("0000000000000000000000000000000000000000000000000000000000000000"),
        height = 1L,
        timestamp = 0L
      )
  )

  /**
    * @param stack Expected stack of VM after program execution
    * @param heap Expected heap of VM after program execution
    * @param effects Expected effect occurred during program execution
    * @param error Expected error occurred during program execution
    */
  final case class Expectations(`watts-spent`: Long,
                                stack: Seq[Primitive] = Nil,
                                heap: Map[Primitive.Ref, Data] = Map.empty,
                                effects: Seq[vm.Effect] = Nil,
                                error: Option[vm.Error] = None)

  final case class ExpectationsWithoutWatts(stack: Seq[Primitive] = Nil,
                                            heap: Map[Primitive.Ref, Data] = Map.empty,
                                            effects: Seq[vm.Effect] = Nil,
                                            error: Option[vm.Error] = None)

  object ExpectationsWithoutWatts {

    def fromExpectations(e: Expectations): ExpectationsWithoutWatts =
      ExpectationsWithoutWatts(e.stack, e.heap, e.effects, e.error)
  }

  def heap(input: VmSandbox.Preconditions): ArrayBuffer[Data] = {
    if (input.heap.nonEmpty) {
      val length = input.heap.map(_._1.data).max + 1
      val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
      input.heap.foreach { case (ref, value) => buffer(ref.data) = value }
      buffer
    } else {
      ArrayBuffer[Data]()
    }
  }

  def environment(input: VmSandbox.Preconditions, effects: mutable.Buffer[vm.Effect], pExecutor: Address): Environment =
    new VmSandbox.EnvironmentSandbox(
      effects,
      input.`program-storage`,
      input.balances.toSeq,
      input.programs.toSeq,
      pExecutor,
      input.`app-state-info`
    )

  def run(input: VmSandbox.Preconditions, code: ByteString): VmSandbox.Expectations = {
    val sandboxVm = new VmImpl()
    val heapSandbox = heap(input)
    val memory = MemoryImpl(ArrayBuffer(input.stack: _*), heapSandbox)
    val wattCounter = new WattCounterImpl(input.`watts-limit`)

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val effects = mutable.Buffer[vm.Effect]()
    val environmentS: Environment = environment(input, effects, pExecutor)
    val storage = new VmSandbox.StorageSandbox(Address.Void, effects, input.storage.toSeq)

    val error = Try {
      memory.enterProgram(Address.Void)
      sandboxVm.runBytes(
        code.asReadOnlyByteBuffer(),
        environmentS,
        memory,
        wattCounter,
        Some(storage),
        Some(Address.Void),
        pcallAllowed = true
      )
      memory.exitProgram()
    }.fold(
      {
        case e: Data.DataException =>
          Some(
            RuntimeException(
              DataError(e.getMessage),
              FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap),
              memory.callStack,
              memory.currentCounter
            ))
        case ThrowableVmError(e) =>
          Some(
            RuntimeException(
              e,
              FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap),
              memory.callStack,
              memory.currentCounter))
      },
      _ => None
    )

    Expectations(
      wattCounter.spent,
      memory.stack,
      memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap,
      effects,
      error.map(_.error)
    )
  }
}
