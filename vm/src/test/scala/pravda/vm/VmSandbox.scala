package pravda.vm
import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm
import pravda.vm.Data.Primitive

import scala.collection.mutable

object VmSandbox {

  class EnvironmentSandbox(effects: mutable.Buffer[vm.Effect],
                           initStorages: Map[Address, Map[Primitive, Data]],
                           initBalances: Seq[(Address, Primitive.BigInt)],
                           initPrograms: Seq[(Address, Primitive.Bytes)],
                           pExecutor: Address)
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
      val balance = NativeCoin @@ balances.get(address).fold(0L)(_.data.toLong)
      effects += vm.Effect.ShowBalance(address, balance)
      balance
    }

    def transfer(from: Address, to: Address, amount: NativeCoin): Unit = {
      val fromBalance = balances.get(from).fold(scala.BigInt(0))(_.data)
      val toBalance = balances.get(to).fold(scala.BigInt(0))(_.data)
      balances(from) = Data.Primitive.BigInt(fromBalance - amount)
      balances(to) = Data.Primitive.BigInt(toBalance + amount)
      effects += vm.Effect.Transfer(from, to, amount)
    }

    def event(address: Address, name: String, data: MarshalledData): Unit =
      effects += vm.Effect.Event(address, name, data)
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
}
