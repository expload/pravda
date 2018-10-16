package pravda.vm
import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm
import pravda.vm.Data.Primitive

import scala.collection.mutable
import scala.util.Random

object VmSandbox {

  class EnvironmentSandbox(effects: mutable.Buffer[vm.Effect],
                           initBalances: Seq[(Address, Primitive.BigInt)],
                           initPrograms: Seq[(Address, Primitive.Bytes)],
                           pExecutor: Address)
      extends Environment {

    val balances = mutable.Map(initBalances: _*)
    val programs = mutable.Map(initPrograms: _*)
    val sealedPrograms = mutable.Map[Address, Boolean]()

    def executor: Address = pExecutor

    def sealProgram(address: Address): Unit = {
      sealedPrograms(address) = true
      effects += vm.Effect.ProgramSeal(address)

    }

    def updateProgram(address: Address, code: ByteString): Unit = {
      if (sealedPrograms.get(address).exists(identity)) {
        programs(address) = Data.Primitive.Bytes(code)
        effects += vm.Effect.ProgramUpdate(address, Data.Primitive.Bytes(code))
      }
    }

    def createProgram(owner: Address, code: ByteString): Address = {
      val randomBytes = new Array[Byte](32)
      Random.nextBytes(randomBytes)
      val address = Address @@ ByteString.copyFrom(randomBytes)
      programs(address) = Data.Primitive.Bytes(code)
      effects += vm.Effect.ProgramCreate(address, Data.Primitive.Bytes(code))
      address
    }

    def getProgram(address: Address): Option[ProgramContext] = {
      programs.get(address).map { p =>
        ProgramContext(
          new Storage { // TODO meaningful storage
            override def get(key: Data): Option[Data] = None
            override def put(key: Data, value: Data): Option[Data] = None
            override def delete(key: Data): Option[Data] = None
          },
          p.data.asReadOnlyByteBuffer()
        )
      }
    }

    def getProgramOwner(address: Address): Option[Address] =
      Some(pExecutor) // TODO display actual owner for created programs

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
    override def event(address: Address, name: String, data: Data): Unit =
      effects += vm.Effect.Event(address, name, data)
  }

  class StorageSandbox(effects: mutable.Buffer[vm.Effect], initStorage: Seq[(Data, Data)]) extends Storage {
    val storageItems: mutable.Map[Data, Data] = mutable.Map(initStorage: _*)

    override def get(key: Data): Option[Data] = {
      val value = storageItems.get(key)
      effects += vm.Effect.StorageRead(Address.Void, key, value)
      value
    }

    override def put(key: Data, value: Data): Option[Data] = {
      val prev = storageItems.put(key, value)
      effects += vm.Effect.StorageWrite(Address.Void, key, prev, value)
      prev
    }

    override def delete(key: Data): Option[Data] = {
      val prev = storageItems.remove(key)
      effects += vm.Effect.StorageRemove(Address.Void, key, prev)
      prev
    }
  }
}
