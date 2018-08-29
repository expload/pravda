package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import fastparse.StringReprOps
import fastparse.core.{Parsed, Parser}
import pravda.common.DiffUtils
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm.Data.Primitive
import pravda.vm.VmSandbox.EnvironmentEffect._
import pravda.vm.asm.{Operation, PravdaAssembler}
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object VmSandbox {

  final case class Macro(name: String, args: List[String])

  type MacroHandler = PartialFunction[Macro, Seq[Operation]]

  final case class Case(program: Either[Macro, Seq[Operation]],
                        expectations: Expectations,
                        preconditions: Preconditions)

  final case class Preconditions(balances: Map[Address, Primitive.BigInt],
                                 watts: Long = 0,
                                 memory: Memory = Memory(),
                                 storage: Map[Primitive, Data] = Map.empty)

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

  final case class Expectations(watts: Long, memory: Memory, effects: Seq[EnvironmentEffect], error: Option[String])

  val parser: Parser[Case, Char, String] = {

    import Data.parser._
    import PravdaAssembler.{parser => assemblerParser}
    import fastparse.all._

    val space = P(CharIn("\r\t\n ").rep())
    val `=` = P(space ~ "=" ~ space)
    val ws = P(CharIn("\r\t\n ").rep(min = 1))
    val notws = P(CharsWhile(!"\r\t\n ".contains(_)))
    val `,` = P(space ~ "," ~ space)
    val alpha = P(CharIn('a' to 'z', 'A' to 'Z', "_").rep(1).!)

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
      P("preconditions:" ~/ space ~ balances.? ~ space ~ watts ~ space ~ memory.? ~ space ~ storage.?).map {
        case (b, w, m, s) =>
          Preconditions(
            balances = b.getOrElse(Nil).toMap,
            watts = w.toLong,
            memory = m.getOrElse(Memory()),
            storage = s.getOrElse(Nil).toMap
          )
      }
    }

    val expectations = {

      val watts = P("watts-spent:" ~/ space ~ int)

      val effects = {

        val sput = P("sput" ~/ ws ~ primitive ~ ws ~ all).map {
          case (k, v) => EnvironmentEffect.StoragePut(k, v)
        }

        val sget = P("sget" ~/ ws ~ primitive ~ (ws ~ all).?).map {
          case (k, v) => EnvironmentEffect.StorageGet(k, v)
        }

        val sdel = P("sdel" ~/ ws ~ primitive).map(EnvironmentEffect.StorageDelete)

        val pcreate = P("pcreate" ~/ ws ~ address ~ ws ~ address ~ ws ~ bytes).map {
          case (owner, addr, program) => EnvironmentEffect.ProgramCreate(owner, addr, program.data)
        }

        val pupdate = P("pupdate" ~/ ws ~ address ~ ws ~ bytes).map {
          case (addr, program) => EnvironmentEffect.ProgramUpdate(addr, program.data)
        }

        val transfer = P("transfer" ~/ ws ~ address ~ ws ~ address ~ ws ~ bigint).map {
          case (from, to, amount) => EnvironmentEffect.BalanceTransfer(from, to, NativeCoin @@ amount.data.toLong)
        }

        val balance = P("balance" ~/ ws ~ address ~ ws ~ bigint).map {
          case (addr, amount) => EnvironmentEffect.BalanceGet(addr, NativeCoin @@ amount.data.toLong)
        }

        P("effects:" ~/ space ~ (sput | sget | sdel | pcreate | pupdate | transfer | balance).rep(sep = `,`))
      }

      val error = P("error:" ~/ space ~ (space ~ "|" ~ CharsWhile(_ != '\n').! ~ "\n").rep).map(_.mkString("\n")).?

      P("expectations:" ~/ space ~ watts ~ space ~ memory ~ space ~ effects.? ~ space ~ error).map {
        case (w, mem, eff, err) => Expectations(w.toLong, mem, eff.getOrElse(Nil), err)
      }
    }

    val `macro` = ("#" ~/ alpha ~ ws ~ (notws.! ~ space).rep()).map { case (name, args) => Macro(name, args.toList) }
    val program = `macro`.map(Left(_)) | assemblerParser.map(Right(_))

    P(
      Start ~ space
        ~ preconditions ~ space
        ~ expectations ~ space
        ~ "-".rep(min = 3) ~ space
        ~ program ~ End).map {
      case (p, exp, prog) =>
        Case(prog, exp, p)
    }
  }

  def printExpectations(e: Expectations): String = {
    def printData(d: Data) = d.mkString(pretty = true).replace('\n', ' ')

    //def printAddress(address: Address) = printData(Data.Primitive.Bytes(address))

    def printEffect(effect: EnvironmentEffect) = effect match {
      case StoragePut(key, value)                 => s"sput ${printData(key)} ${printData(value)}"
      case StorageGet(key, value)                 => s"sget ${printData(key)}${value.fold("")(" " + printData(_))}"
      case StorageDelete(key)                     => s"sdel ${printData(key)}"
      case ProgramCreate(owner, address, program) => ???
      case ProgramUpdate(address, program)        => ???
      case ProgramSeal(address)                   => ???
      case BalanceGet(address, coins)             => s"balance x${pravda.common.bytes.byteString2hex(address)} $coins"
      case BalanceAccrue(address, coins)          => ???
      case BalanceWithdraw(address, coins)        => ???
      case BalanceTransfer(from, to, coins)       => ???
      // TODO implement printing of all other effects
    }

    s"""
       |watts-spent: ${e.watts}
       |stack:
       |  ${e.memory.stack.map(printData).mkString(", ")}
       |heap:
       |  ${e.memory.heap.map { case (k, v) => s"${printData(k)} = ${printData(v)}" }.mkString(",\n  ")}
       |effects:
       |  ${e.effects.map(printEffect).mkString(",\n  ")}
       |${e.error.fold("")(vmError => s"error: \n${vmError.split('\n').map("||" + _).mkString("\n")}\n")}""".stripMargin
  }

  def parseCase(text: String): Either[String, Case] = {
    parser.parse(text) match {
      case Parsed.Success(c, _) =>
        Right(c)
      case Parsed.Failure(_, index, extra) =>
        val in = extra.input
        def aux(start: Int, i: Int, lim: Int): String = {
          if (lim > 0 && i < text.length
              && text.charAt(i) != '\n'
              && text.charAt(i) != '\r'
              && text.charAt(i) != ' ') aux(start, i + 1, lim - 1)
          else text.substring(start, i - 1)
        }
        val pos = StringReprOps.prettyIndex(in, index)
        val found = aux(index, index, 20)
        Left(s"$pos: ${extra.traced.expected} expected but '$found' found.")
    }
  }

  def assertCase(c: Case, macroHandler: MacroHandler = PartialFunction.empty): Unit = {
    val vm = new VmImpl()
    val ops = c.program match {
      case Left(m) =>
        assert(macroHandler.isDefinedAt(m), s"Unknown macro: #${m.name} ${m.args.mkString(" ")}")
        macroHandler(m)
      case Right(o) => o
    }
    val program = PravdaAssembler.assemble(ops, saveLabels = true)
    val heap = {
      if (c.preconditions.memory.heap.nonEmpty) {
        val length = c.preconditions.memory.heap.map(_._1.data).max + 1
        val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
        c.preconditions.memory.heap.foreach { case (ref, value) => buffer(ref.data) = value }
        buffer
      } else {
        ArrayBuffer[Data]()
      }
    }
    val memory = MemoryImpl(ArrayBuffer(c.preconditions.memory.stack: _*), heap)
    val wattCounter = new WattCounterImpl(c.preconditions.watts)

    val effects = mutable.Buffer[EnvironmentEffect]()

    val environment: Environment = new Environment {
      val balances = mutable.Map(c.preconditions.balances.toSeq: _*)

      def executor: Address =
        Address @@ ByteString.copyFrom(Array.fill[Byte](32)(0x00))

      def sealProgram(address: Address): Unit =
        effects += EnvironmentEffect.ProgramSeal(address)

      def updateProgram(address: Address, code: ByteString): Unit =
        effects += EnvironmentEffect.ProgramUpdate(address, code)

      def createProgram(owner: Address, code: ByteString): Address = {
        val address = Address @@ ByteString.EMPTY // TODO generate
        effects += EnvironmentEffect.ProgramCreate(owner, address, code)
        address
      }

      def getProgram(address: Address): Option[ProgramContext] =
        None // TODO program mocking

      def getProgramOwner(address: Address): Option[Address] =
        None // TODO program mocking

      def balance(address: Address): NativeCoin = {
        val balance = NativeCoin @@ balances.get(address).fold(0L)(_.data.toLong)
        effects += EnvironmentEffect.BalanceGet(address, balance)
        balance
      }

      // TODO Method is too complex. we should move functionality to NativeCoinOperations
      def transfer(from: Address, to: Address, amount: NativeCoin): Unit = {
        val fromBalance = balances.get(from).fold(scala.BigInt(0))(_.data)
        val toBalance = balances.get(to).fold(scala.BigInt(0))(_.data)
        balances(from) = Data.Primitive.BigInt(fromBalance - amount)
        balances(to) = Data.Primitive.BigInt(toBalance + amount)
        effects += EnvironmentEffect.BalanceTransfer(from, to, amount)
      }
    }

    val storage: Storage = new Storage {
      val storageItems: mutable.Map[Data, Data] = mutable.Map[Data, Data](c.preconditions.storage.toSeq: _*)

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
      vm.spawn(ByteBuffer.wrap(program.toByteArray), environment, memory, wattCounter, Some(storage), None, false)

    DiffUtils.assertEqual(
      printExpectations(
        Expectations(
          res.wattCounter.spent,
          Memory(
            res.memory.stack,
            res.memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap
          ),
          effects,
          res.error.map(_.mkString)
        )
      ),
      printExpectations(c.expectations)
    )
  }
}
