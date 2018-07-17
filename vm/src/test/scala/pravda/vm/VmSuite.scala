//package pravda.vm
//
//import com.google.protobuf.ByteString
//import fastparse.StringReprOps
//import fastparse.core.{Parsed, Parser}
//import pravda.common.domain.{Address, NativeCoin}
//import pravda.vm.Data.Primitive
//import pravda.vm.asm.{Operation, PravdaAssembler}
//import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
//import utest._
//import utest.framework.{TestCallTree, Tree}
//
//import scala.collection.mutable
//import scala.io.Source
//
//object VmSuite extends TestSuite {
//
//  import Data.Primitive._
//
//  final case class Case(name: String,
//                        program: Seq[Operation],
//                        expectations: Expectations,
//                        preconditions: Preconditions)
//
//  final case class Preconditions(balances: Map[Address, Primitive.BigInt],
//                                 watts: Long = 0,
//                                 memory: Memory = Memory(),
//                                 storage: Map[Primitive, Data] = Map.empty)
//
//  final case class Memory(stack: Seq[Data.Primitive] = Nil,
//                          heap: Map[Ref, Data] = Map.empty)
//
//  final case class Expectations(watts: Long ,memory: Memory, effects: Seq[EnvironmentEffect])
//
//
//  val parser: Parser[Case, Char, String] = {
//
//    import fastparse.all._
//    import Data.parser._
//    import PravdaAssembler.{parser => assemblerParser}
//
//    val space = P(CharIn("\r\t\n ").rep())
//    val `=` = P(space ~ "=" ~ space)
//    val ` ` = P(CharIn("\r\t\n ").rep(min = 1))
//    val `,` = P(space ~ "," ~ space)
//
//    val memory = {
//      val stack = P("stack:" ~/ space ~ primitive.rep(sep = `,`))
//      val heap = P("heap:" ~/ space ~ (ref ~ `=` ~ all).rep(sep = `,`))
//      P(stack.? ~ space ~ heap.?).log().map {
//        case (maybeStack, maybeHeap) =>
//          Memory(
//            maybeStack.getOrElse(Nil),
//            maybeHeap.getOrElse(Nil).toMap
//          )
//      }
//    }
//
//    val address = P(bytes).map(Address @@ _.data)
//
//    val preconditions = {
//      val watts = P("watts-limit:" ~/ space ~ uint)
//      val balances = P("balances: " ~/ space ~ (address ~ `=` ~ bigint).rep)
//      val storage = P("storage:" ~/ space ~ (primitive ~ `=` ~ all).rep(sep = `,`)).log()
//      P("preconditions:" ~/ space ~ balances.? ~ space ~ watts ~ space ~ memory.? ~ space ~ storage.?).log().map {
//        case (b, w, m, s) => Preconditions(
//          balances = b.getOrElse(Nil).toMap,
//          watts = w.toLong,
//          memory = m.getOrElse(Memory()),
//          storage = s.getOrElse(Nil).toMap
//        )
//      }
//    }
//
//    val expectations = {
//
//      val effects = {
//
//        val sput = P("sput" ~/ ` ` ~ primitive ~ ` ` ~ all).map {
//          case (k, v) => EnvironmentEffect.StoragePut(k, v)
//        }
//
//        val sget = P("sget" ~/ ` ` ~ primitive ~ (` ` ~ all).?).map {
//          case (k, v) => EnvironmentEffect.StorageGet(k, v)
//        }
//
//        val pcreate = P("pcreate" ~/ ` ` ~ address ~ ` ` ~ address ~ ` ` ~ bytes).map {
//          case (owner, addr, program) => EnvironmentEffect.ProgramCreate(owner, addr, program.data)
//        }
//
//        val pupdate = P("pupdate" ~/ ` ` ~ address ~ ` ` ~ bytes).map {
//          case (addr, program) => EnvironmentEffect.ProgramUpdate(addr, program.data)
//        }
//
//        val transfer = P("transfer" ~/ ` ` ~ address ~ ` ` ~ address ~ ` ` ~ bigint).map {
//          case (from, to, amount) => ???
//        }
//
//        val balance = P("balance" ~/ ` ` ~ address ~ ` ` ~ bigint).map {
//          case (addr, amount) => EnvironmentEffect.BalanceGet(addr, NativeCoin @@ amount.data.toLong)
//        }
//
//        P("effects:" ~/ space ~ (sput | sget | pcreate | pupdate | transfer | balance).rep(sep = `,`))
//      }
//
//      val watts = P("watts-spent:" ~/ space ~ int)
//
//      P("expectations:" ~/ space ~ watts ~ memory ~ space ~ effects.?).map {
//        case (w, mem, eff) => Expectations(w.toLong, mem, eff.getOrElse(Nil))
//      }
//    }
//
//    P(Start ~ space
//      ~ preconditions ~ space
//      ~ expectations ~ space
//      ~ "-".rep(min = 3)
//      ~ assemblerParser ~ End
//    ) map {
//      case (p, exp, ops) =>
//        Case("", ops, exp, p)
//    }
//  }
//
//  def prepareCase(name: String): Either[String, Case] = {
//    val text = Source.fromResource(s"$name.asm").mkString
//    parser.parse(text) match {
//      case Parsed.Success(c, _) =>
//        Right(c.copy(name = name))
//      case Parsed.Failure(_, index, extra) =>
//        val in = extra.input
//        def aux(start: Int, i: Int, lim: Int): String = {
//          if (lim > 0 && i < text.length
//            && text.charAt(i) != '\n'
//            && text.charAt(i) != '\r'
//            && text.charAt(i) != ' ') aux(start, i + 1, lim - 1)
//          else text.substring(start, i - 1)
//        }
//        val pos = StringReprOps.prettyIndex(in, index)
//        val found = aux(index, index, 20)
//        Left(s"$name.asm:$pos: ${extra.traced.expected} expected but '$found' found.")
//        //Left(s"$name.asm: " + extra.input.repr.errorMessage(extra.input, extra.traced.expected, index))
//    }
//  }
//
//  val cases = Seq(
//    "add",
//  )
//
//  val tests: Tests =
//    Tests(
//      Tree[String]("VmSuite", cases.map(`case` => Tree[String](`case`)):_*), // Name tree
//      new TestCallTree(
//        Right {
//          cases.map(prepareCase).toVector.map {
//            case Left(value) => throw new java.lang.AssertionError(value)
//            case Right(c) =>
//              val vm = new VmImpl()
//              val program = PravdaAssembler.assemble(c.program, saveLabels = true)
//              val memory = MemoryImpl.empty
//              val wattCounter = new WattCounterImpl(c.preconditions.watts)
//              val environment: Environment = new Environment {
//
//                val balances = mutable.Map(c.preconditions.balances.toSeq:_*)
//                val effects = mutable.Buffer[EnvironmentEffect]()
//
//                def executor: Address =
//                  Address @@ ByteString.EMPTY // TODO
//
//                def updateProgram(address: Address, code: ByteString): Unit =
//                  effects += EnvironmentEffect.ProgramUpdate(address, code)
//
//                def createProgram(owner: Address, code: ByteString): Address = {
//                  val address = Address @@ ByteString.EMPTY // TODO generate
//                  effects += EnvironmentEffect.ProgramCreate(owner, address, code)
//                  address
//                }
//
//                def getProgram(address: Address): Option[ProgramContext] =
//                  None // TODO program mocking
//
//                def getProgramOwner(address: Address): Option[Address] =
//                  None // TODO program mocking
//
//                def balance(address: Address): NativeCoin = {
//                  val balance = NativeCoin @@ balances.get(address).fold(0L)(_.data.toLong)
//                  effects += EnvironmentEffect.BalanceGet(address, balance)
//                  balance
//                }
//
//                // TODO Method is too complex. we should move functionality to NativeCoinOperations
//                def transfer(from: Address, to: Address, amount: NativeCoin): Unit = ???
//
//                def accrue(address: Address, amount: NativeCoin): Unit = {
//                  val b = balances.get(address).fold(scala.BigInt(0))(_.data)
//                  balances(address) = BigInt(b + amount)
//                  effects += EnvironmentEffect.BalanceAccrue(address, amount)
//                }
//
//                def withdraw(address: Address, amount: NativeCoin): Unit = {
//                  val b = balances.get(address).fold(scala.BigInt(0))(_.data)
//                  balances(address) = BigInt(b - amount)
//                  effects += EnvironmentEffect.BalanceWithdraw(address, amount)
//                }
//              }
//
//              vm.spawn(program, environment, memory, wattCounter, )
//              new TestCallTree(Left(value)) // TODO
//          }
//        }
//      )
//    )
//}
