package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import fastparse.all._
import pravda.common.domain.{Address, NativeCoin}
import pravda.common.{bytes => byteUtils}
import pravda.vm.Data.Primitive
import pravda.vm.VmSandbox.EnvironmentEffect._
import pravda.vm.asm.{Operation, PravdaAssembler}
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import scala.language.higherKinds

object VmSandbox {

  final case class Macro(name: String, args: List[String])

  type MacroHandler = PartialFunction[Macro, Seq[Operation]]

  final case class Case(program: Option[Seq[Operation]] = None, preconditions: Option[Preconditions] = None)

  final case class Preconditions(balances: Map[Address, Primitive.BigInt],
                                 watts: Long = 0,
                                 memory: Memory = Memory(),
                                 storage: Map[Primitive, Data] = Map.empty,
                                 programs: Map[Address, Primitive.Bytes] = Map.empty)

  final case class Memory(stack: Seq[Data.Primitive] = Nil, heap: Map[Data.Primitive.Ref, Data] = Map.empty)

  sealed trait EnvironmentEffect

  object EnvironmentEffect {
    final case class StoragePut(key: Data, value: Data)                                   extends EnvironmentEffect
    final case class StorageGet(key: Data, value: Option[Data])                           extends EnvironmentEffect
    final case class StorageDelete(key: Data)                                             extends EnvironmentEffect
    final case class ProgramCreate(owner: Address, address: Address, program: ByteString) extends EnvironmentEffect
    final case class ProgramUpdate(address: Address, program: ByteString)                 extends EnvironmentEffect
    final case class ProgramSeal(address: Address)                                        extends EnvironmentEffect
    final case class BalanceGet(address: Address, coins: NativeCoin)                      extends EnvironmentEffect
    final case class BalanceAccrue(address: Address, coins: NativeCoin)                   extends EnvironmentEffect
    final case class BalanceWithdraw(address: Address, coins: NativeCoin)                 extends EnvironmentEffect
    final case class BalanceTransfer(from: Address, to: Address, coins: NativeCoin)       extends EnvironmentEffect
  }

  final case class EnviromentEvent(address: Address, name: String, data: Data)

  final case class Expectations(watts: Long,
                                memory: Memory,
                                effects: Seq[EnvironmentEffect],
                                events: Seq[EnviromentEvent],
                                error: Option[String])

  val (preconditions, program) = {
    import Data.parser._
    import PravdaAssembler.{parser => assemblerParser}

    val space = P(CharIn("\r\t\n ").rep())
    val `=` = P(space ~ "=" ~ space)
    //val ws = P(CharIn("\r\t\n ").rep(min = 1))
    //val notws = P(CharsWhile(!"\r\t\n ".contains(_)))
    val `,` = P(space ~ "," ~ space)
    //val alpha = P(CharIn('a' to 'z', 'A' to 'Z', "_").rep(1).!)

    val memory = {
      val stack = P("stack:" ~/ space ~ primitive.rep(sep = `,`))
      val heap = P("heap:" ~/ space ~ (ref ~ `=` ~ all).rep(sep = `,`))
      P(stack.? ~ space ~ heap.?).map {
        case (maybeStack, maybeHeap) =>
          Memory(
            maybeStack.getOrElse(Nil),
            maybeHeap.getOrElse(Nil).toMap
          )
      }
    }

    val address = P(bytes).map(Address @@ _.data)

    val preconditions = {
      val watts = P("watts-limit:" ~/ space ~ uint)
      val balances = P("balances:" ~/ space ~ (address ~ `=` ~ bigint).rep)
      val storage = P("storage:" ~/ space ~ (primitive ~ `=` ~ all).rep(sep = `,`))
      val programs = P("programs:" ~/ space ~ (address ~ `=` ~ bytes)).rep(sep = `,`)
      P(space ~ balances.? ~ space ~ watts ~ space ~ memory.? ~ space ~ storage.? ~ space ~ programs.?)
        .map {
          case (b, w, m, s, ps) =>
            Preconditions(
              balances = b.getOrElse(Nil).toMap,
              watts = w.toLong,
              memory = m.getOrElse(Memory()),
              storage = s.getOrElse(Nil).toMap,
              programs = ps.getOrElse(Nil).toMap
            )
        }
    }

    val program = assemblerParser

    (preconditions, program)
  }

  def printExpectations(e: Expectations): String = {
    def printData(d: Data) = d.mkString(pretty = true).replace("\n", "")

    def printEffect(effect: EnvironmentEffect) = effect match {
      case StoragePut(key, value)                 => s"sput ${printData(key)} ${printData(value)}"
      case StorageGet(key, value)                 => s"sget ${printData(key)}${value.fold("")(" " + printData(_))}"
      case StorageDelete(key)                     => s"sdel ${printData(key)}"
      case ProgramCreate(owner, address, program) => ???
      case ProgramUpdate(address, program)        => ???
      case ProgramSeal(address)                   => ???
      case BalanceGet(address, coins)             => s"balance x${byteUtils.byteString2hex(address)} $coins"
      case BalanceAccrue(address, coins)          => ???
      case BalanceWithdraw(address, coins)        => ???
      case BalanceTransfer(from, to, coins)       => ???
      // TODO implement printing of all other effects
    }

    def printEvent(event: EnviromentEvent) = event match {
      case VmSandbox.EnviromentEvent(address, name, data) =>
        s"x${byteUtils.byteString2hex(address)} $name ${data.mkString()}"
    }

    def combine(ops: Seq[(String, Option[String])]): String =
      ops
        .flatMap { op =>
          for {
            text <- op._2
          } yield s"""|${op._1}:
              ${text.split('\n').map("|  " + _).mkString("\n")}""".stripMargin
        }
        .mkString("\n")

    def nonEmptyReduce[T, A[T] <: Iterable[T], B](a: A[T])(reduce: A[T] => B): Option[B] =
      if (a.isEmpty) {
        None
      } else {
        Some(reduce(a))
      }

    s"watts-spent: ${e.watts}\n" +
      combine(
        Seq(
          "stack" -> nonEmptyReduce(e.memory.stack)(_.map(printData).mkString(", ")),
          "heap" -> nonEmptyReduce(e.memory.heap.toSeq)(_.map { case (k, v) => s"${printData(k)} = ${printData(v)}" }
            .mkString(",\n")),
          "effects" -> nonEmptyReduce(e.effects)(_.map(printEffect).mkString(",\n")),
          "events" -> nonEmptyReduce(e.events)(_.map(printEvent).mkString(",\n")),
          "error" -> nonEmptyReduce(e.error.toList)(_.head.split('\n').map("|" + _).mkString("\n"))
        ))
  }

  def sandboxRun(ops: Seq[Operation], pre: Preconditions): Expectations = {
    val vm = new VmImpl()
    val asmProgram = PravdaAssembler.assemble(ops, saveLabels = true)
    val heap = {
      if (pre.memory.heap.nonEmpty) {
        val length = pre.memory.heap.map(_._1.data).max + 1
        val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
        pre.memory.heap.foreach { case (ref, value) => buffer(ref.data) = value }
        buffer
      } else {
        ArrayBuffer[Data]()
      }
    }
    val memory = MemoryImpl(ArrayBuffer(pre.memory.stack: _*), heap)
    val wattCounter = new WattCounterImpl(pre.watts)

    val effects = mutable.Buffer[EnvironmentEffect]()
    val events = mutable.Map[(Address, String), mutable.Buffer[Data]]()

    val pExecutor = Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)

    val environment: Environment = new Environment {
      val balances = mutable.Map(pre.balances.toSeq: _*)
      val programs = mutable.Map(pre.programs.toSeq: _*)
      val sealedPrograms = mutable.Map[Address, Boolean]()

      def executor: Address = pExecutor

      def sealProgram(address: Address): Unit = {
        sealedPrograms(address) = true
        effects += EnvironmentEffect.ProgramSeal(address)
      }

      def updateProgram(address: Address, code: ByteString): Unit = {
        if (sealedPrograms.get(address).exists(identity)) {
          programs(address) = Data.Primitive.Bytes(code)
          effects += EnvironmentEffect.ProgramUpdate(address, code)
        }
      }

      def createProgram(owner: Address, code: ByteString): Address = {
        val randomBytes = new Array[Byte](32)
        Random.nextBytes(randomBytes)
        val address = Address @@ ByteString.copyFrom(randomBytes)
        programs(address) = Data.Primitive.Bytes(code)
        effects += EnvironmentEffect.ProgramCreate(owner, address, code)
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
        effects += EnvironmentEffect.BalanceGet(address, balance)
        balance
      }

      def transfer(from: Address, to: Address, amount: NativeCoin): Unit = {
        val fromBalance = balances.get(from).fold(scala.BigInt(0))(_.data)
        val toBalance = balances.get(to).fold(scala.BigInt(0))(_.data)
        balances(from) = Data.Primitive.BigInt(fromBalance - amount)
        balances(to) = Data.Primitive.BigInt(toBalance + amount)
        effects += EnvironmentEffect.BalanceTransfer(from, to, amount)
      }
      override def event(address: Address, name: String, data: Data): Unit = {
        val key = (address, name)
        if (!events.contains(key)) {
          events(key) = mutable.Buffer.empty
        }
        events(key) += data
      }
    }

    val storage: Storage = new Storage {
      val storageItems: mutable.Map[Data, Data] = mutable.Map[Data, Data](pre.storage.toSeq: _*)

      override def get(key: Data): Option[Data] = {
        val value = storageItems.get(key)
        effects += EnvironmentEffect.StorageGet(key, value)
        value
      }

      override def put(key: Data, value: Data): Option[Data] = {
        val prev = storageItems.put(key, value)
        effects += EnvironmentEffect.StoragePut(key, value)
        prev
      }

      override def delete(key: Data): Option[Data] = {
        val prev = storageItems.remove(key)
        effects += EnvironmentEffect.StorageDelete(key)
        prev
      }
    }

    val res =
      vm.spawn(ByteBuffer.wrap(asmProgram.toByteArray),
               environment,
               memory,
               wattCounter,
               Some(storage),
               Some(Address.Void),
               true)

    Expectations(
      res.wattCounter.spent,
      Memory(
        res.memory.stack,
        res.memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap
      ),
      effects,
      events.flatMap {
        case ((address, name), datas) => datas.map(EnviromentEvent(address, name, _))
      }.toSeq,
      res.error.map(_.mkString)
    )
  }
}
