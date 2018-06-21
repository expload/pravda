package pravda.test.utils

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.forth.Compiler
import pravda.vm._
import pravda.vm.serialization._
import pravda.vm.state._

import scala.collection.mutable

object IntegrationUtils {

  trait StackItem[T] {
    def get(item: Data): T
  }

  object StackItem {

    implicit val intStackItem: StackItem[Int] =
      (item: Data) => ByteBuffer.wrap(item.toByteArray).getInt

    implicit val floatStackItem: StackItem[Double] =
      (item: Data) => ByteBuffer.wrap(item.toByteArray).getDouble

    implicit val byteStringStackItem: StackItem[Data] =
      (item: Data) => item

    implicit val boolStackItem: StackItem[Boolean] =
      (item: ByteString) => if ((ByteBuffer.wrap(item.toByteArray).get & 0xFF) == 1) true else false

    implicit val arrayStackItem: StackItem[List[Byte]] =
      (item: ByteString) => item.toByteArray.toList
  }

  def runWithoutEnviroment[T](code: Array[Byte])(implicit stackItem: StackItem[T]): List[T] = {
    val emptyState = new Environment {
      override def getProgram(address: Address): Option[ProgramContext] = ???
      override def updateProgram(address: Address, code: Data): Data = ???
      override def createProgram(owner: Address, code: Data): Address = ???
      override def getProgramOwner(address: Address): Option[Address] = ???
      override def transfer(from: Address, to: Address, amount: NativeCoin): Unit = ???
      override def balance(address: Address): NativeCoin = ???
      override def withdraw(address: Address, amount: NativeCoin): Unit = ???
      override def accrue(address: Address, amount: NativeCoin): Unit = ???
    }
    val stack = Vm.runRaw(ByteString.copyFrom(code), Address @@ ByteString.EMPTY, emptyState, Long.MaxValue).memory.stack
    stack.map(stackItem.get).toList
  }

  def runForthWithoutEnviroment[T](code: String)(implicit stackItem: StackItem[T]): Either[String, List[T]] =
    Compiler().compile(code).map(c => runWithoutEnviroment(c))

  def runProgram[T](
      c: Array[Byte],
      storageItems: Seq[(Data, Data)] = Seq.empty,
      executor: Address = Address @@ ByteString.EMPTY)(implicit stackItem: StackItem[T]): (List[T], Map[Data, T]) = {
    val programAddress = Address @@ ByteString.copyFrom(Array[Byte](1, 2, 3))

    val programStorageMap = mutable.Map[Data, Data](storageItems: _*)
    val programStorage = new Storage {
      override def get(key: Data): Option[Data] = programStorageMap.get(key)
      override def put(key: Data, value: Data): Option[Data] = programStorageMap.put(key, value)
      override def delete(key: Data): Option[Data] = programStorageMap.remove(key)
    }

    val stateWithAccount = new Environment {
      override def getProgram(address: Address): Option[ProgramContext] =
        if (address == programAddress) {
          Some(new ProgramContext {
            override def storage: Storage = programStorage
            override def code: ByteBuffer = ByteBuffer.wrap(c)
          })
        } else {
          None
        }

      override def updateProgram(address: Address, code: Data): Data = ???
      override def createProgram(owner: Address, code: Data): Address = ???
      override def getProgramOwner(address: Address): Option[Address] = ???
      override def transfer(from: Address, to: Address, amount: NativeCoin): Unit = ???
      override def balance(address: Address): NativeCoin = ???
      override def withdraw(address: Address, amount: NativeCoin): Unit = ???
      override def accrue(address: Address, amount: NativeCoin): Unit = ???
    }
    val memory = MemoryImpl.empty
    Vm.runProgram(programAddress, memory, executor, stateWithAccount, 0, new WattCounter(Long.MaxValue))
    (memory.stack.map(stackItem.get).toList, programStorageMap.mapValues(stackItem.get).toMap)
  }

  def runForthProgram[T](code: String,
                         storageItems: Seq[(Address, Data)] = Seq.empty,
                         executor: Address = Address @@ ByteString.EMPTY)(
      implicit stackItem: StackItem[T]): Either[String, (List[T], Map[Data, T])] =
    Compiler().compile(code).map(c => runProgram(c, storageItems, executor))

  def runAsmProgram[T](code: Seq[asm.Op],
                       storageItems: Seq[(Data, Data)] = Seq.empty,
                       executor: Address = Address @@ ByteString.EMPTY)(
      implicit stackItem: StackItem[T]): (List[T], Map[Data, T]) =
    runProgram(asm.Assembler().compile(code), storageItems, executor)

  def runWithEnviroment[T](code: Array[Byte],
                           from: Address,
                           env: Map[Address, (String, Map[Data, Data])] = Map.empty)
    : (Seq[Data], Map[Address, (ByteString, Map[Data, Data])]) = {
    val enviromentMap = mutable.Map.empty[Address, (ByteBuffer, mutable.Map[Data, Data])]
    var addressCounter = 0

    val enviroment = new Environment {
      override def getProgram(address: Address): Option[ProgramContext] = enviromentMap.get(address) map {
        case (accountCode, storageMap) =>
          new ProgramContext {
            override def storage: Storage = new Storage {
              override def get(key: Data): Option[Data] = storageMap.get(key)
              override def put(key: Data, value: Data): Option[Data] = storageMap.put(key, value)
              override def delete(key: Data): Option[Data] = storageMap.remove(key)
            }
            override def code: ByteBuffer = accountCode
          }
      }
      override def updateProgram(address: Address, code: Data): Data = ???
      override def createProgram(owner: Address, code: Data): Address = {
        addressCounter += 1
        val address = Address @@ int32ToByteString(addressCounter)
        enviromentMap.update(address, (ByteBuffer.wrap(code.toByteArray), mutable.Map.empty))
        address
      }
      override def getProgramOwner(address: Address): Option[Address] = ???
      override def transfer(from: Address, to: Address, amount: NativeCoin): Unit = ???
      override def balance(address: Address): NativeCoin = ???
      override def withdraw(address: Address, amount: NativeCoin): Unit = ???
      override def accrue(address: Address, amount: NativeCoin): Unit = ???
    }
    val stack = Vm.runRaw(ByteString.copyFrom(code), from, enviroment, Long.MaxValue).memory.stack
    (stack, enviromentMap.mapValues { case (c, m) => (ByteString.copyFrom(c.array()), m.toMap) }.toMap)
  }

  def runForthWithEnviroment[T](code: String,
                                from: Address,
                                env: Map[Address, (String, Map[Data, Data])] = Map.empty)
    : Either[String, (Seq[Data], Map[Address, (ByteString, Map[Data, Data])])] =
    Compiler().compile(code).map(c => runWithEnviroment(c, from, env))
}
