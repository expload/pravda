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

package pravda.evm.debug

import com.google.protobuf.ByteString
import pravda.common.bytes
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm
import pravda.vm._
import pravda.vm.Data.Primitive
import pravda.vm.impl.{MemoryImpl, WattCounterImpl}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object VmSandboxDebug {

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
        height = 1L))

  def run[L](input: VmSandboxDebug.Preconditions, code: ByteString)(implicit debugger: Debugger[L]): L = {
    val sandboxVm = new VmImplDebug()
    val heap = {
      if (input.heap.nonEmpty) {
        val length = input.heap.map(_._1.data).max + 1
        val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
        input.heap.foreach { case (ref, value) => buffer(ref.data) = value }
        buffer
      } else {
        ArrayBuffer[Data]()
      }
    }
    val memory = MemoryImpl(ArrayBuffer(input.stack: _*), heap)
    val wattCounter = new WattCounterImpl(input.`watts-limit`)

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val effects = mutable.Buffer[vm.Effect]()
    val environment: Environment = new VmSandboxDebug.EnvironmentSandbox(
      effects,
      input.`program-storage`,
      input.balances.toSeq,
      input.programs.toSeq,
      pExecutor,
      input.`app-state-info`
    )
    val storage = new VmSandboxDebug.StorageSandbox(Address.Void, effects, input.storage.toSeq)

    memory.enterProgram(Address.Void)
    val res = sandboxVm.debugBytes(
      code.asReadOnlyByteBuffer(),
      environment,
      memory,
      wattCounter,
      Some(storage),
      Some(Address.Void),
      pcallAllowed = true
    )
    memory.exitProgram()
    res

  }
}
