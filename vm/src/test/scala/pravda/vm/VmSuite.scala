package pravda.vm

import java.io.File

import com.google.protobuf.ByteString
import pravda.plaintest._
import fastparse.all._
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm.Error.DataError
import pravda.vm.asm.{PravdaAssembler, SourceMap}
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.tethys._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Random, Try}

object VmSuiteData {
  final case class Memory(stack: Seq[Data.Primitive] = Nil, heap: Map[Data.Primitive.Ref, Data] = Map.empty)

  final case class Preconditions(balances: Map[Address, Primitive.BigInt],
                                 watts: Long = 0,
                                 memory: Memory = Memory(),
                                 storage: Map[Primitive, Data] = Map.empty,
                                 programs: Map[Address, Primitive.Bytes] = Map.empty,
                                 executor: Option[Address],
                                 code: List[asm.Operation])

  final case class EnviromentEvent(address: Address, name: String, data: Data)

  final case class Expectations(watts: Long, memory: Memory, effects: Seq[vm.Effect], error: Option[RuntimeException])
}

import VmSuiteData._

object VmSuite extends Plaintest[Preconditions, Expectations] {
  lazy val dir = new File("vm/src/test/resources")
  override lazy val ext = "sbox"

  override def produce(input: Preconditions): Either[String, Expectations] = {
    val sandboxVm = new VmImpl()
    val asmProgram = PravdaAssembler.assemble(input.code, saveLabels = true)
    val heap = {
      if (input.memory.heap.nonEmpty) {
        val length = input.memory.heap.map(_._1.data).max + 1
        val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
        input.memory.heap.foreach { case (ref, value) => buffer(ref.data) = value }
        buffer
      } else {
        ArrayBuffer[Data]()
      }
    }
    val memory = MemoryImpl(ArrayBuffer(input.memory.stack: _*), heap)
    val wattCounter = new WattCounterImpl(input.watts)

    val effects = mutable.Buffer[vm.Effect]()

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val environment: Environment = new Environment {
      val balances = mutable.Map(input.balances.toSeq: _*)
      val programs = mutable.Map(input.programs.toSeq: _*)
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
      override def event(address: Address, name: String, data: Data): Unit = {
        val key = (address, name)
        effects += vm.Effect.Event(address, name, data)
      }
    }

    val storage: Storage = new Storage {
      val storageItems: mutable.Map[Data, Data] = mutable.Map[Data, Data](input.storage.toSeq: _*)

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

    val error = Try {
      memory.enterProgram(Address.Void)
      sandboxVm.runBytes(
        asmProgram.asReadOnlyByteBuffer(),
        environment,
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
              memory.currentOffset
            ))
        case ThrowableVmError(e) =>
          Some(
            RuntimeException(
              e,
              FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap),
              memory.callStack,
              memory.currentOffset))
      },
      _ => None
    )

    Right(
      Expectations(
        wattCounter.spent,
        Memory(
          memory.stack,
          memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap
        ),
        effects,
        error
      )
    )
  }
}
