package pravda.vm

object VmSandbox {

//  final case class Macro(name: String, args: List[String])
//
//  type MacroHandler = PartialFunction[Macro, Seq[Operation]]
//
//  final case class Case(program: Option[Seq[Operation]] = None, preconditions: Option[Preconditions] = None)
//
//  final case class Preconditions(balances: Map[Address, Primitive.BigInt],
//                                 watts: Long = 0,
//                                 memory: Memory = Memory(),
//                                 storage: Map[Primitive, Data] = Map.empty,
//                                 programs: Map[Address, Primitive.Bytes] = Map.empty,
//                                 executor: Option[Address])
//
//
//
//  sealed trait EnvironmentEffect
//
//
//
//  val (preconditions, program) = {
//    import Data.parser._
//    import PravdaAssembler.{parser => assemblerParser}
//
//    val space = P(CharIn("\r\t\n ").rep())
//    val `=` = P(space ~ "=" ~ space)
//    //val ws = P(CharIn("\r\t\n ").rep(min = 1))
//    //val notws = P(CharsWhile(!"\r\t\n ".contains(_)))
//    val `,` = P(space ~ "," ~ space)
//    //val alpha = P(CharIn('a' to 'z', 'A' to 'Z', "_").rep(1).!)
//
//    val memory = {
//      val stack = P("stack:" ~/ space ~ primitive.rep(sep = `,`))
//      val heap = P("heap:" ~/ space ~ (ref ~ `=` ~ all).rep(sep = `,`))
//      P(stack.? ~ space ~ heap.?).map {
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
//      val executor = P("executor:" ~/ space ~ address)
//      val watts = P("watts-limit:" ~/ space ~ uint)
//      val balances = P("balances:" ~/ space ~ (address ~ `=` ~ bigint).rep(sep = `,`))
//      val storage = P("storage:" ~/ space ~ (primitive ~ `=` ~ all).rep(sep = `,`))
//      val programs = P("programs:" ~/ space ~ (address ~ `=` ~ bytes)).rep(sep = `,`)
//      P(space ~ executor.? ~ space ~ balances.? ~ space ~ watts ~ space ~ memory.? ~ space ~ storage.? ~ space ~ programs.?)
//        .map {
//          case (e, b, w, m, s, ps) =>
//            Preconditions(
//              balances = b.getOrElse(Nil).toMap,
//              watts = w.toLong,
//              memory = m.getOrElse(Memory()),
//              storage = s.getOrElse(Nil).toMap,
//              programs = ps.getOrElse(Nil).toMap,
//              executor = e
//            )
//        }
//    }
//
//    val program = assemblerParser
//
//    (preconditions, program)
//  }
//
//  def printExpectations(e: Expectations): String = {
//    def printData(d: Data) = d.mkString(pretty = true).replace("\n", "")
//
//    def printEffect(effect: EnvironmentEffect) = effect match {
//      case StoragePut(key, value)                 => s"sput ${printData(key)} ${printData(value)}"
//      case StorageGet(key, value)                 => s"sget ${printData(key)}${value.fold("")(" " + printData(_))}"
//      case StorageDelete(key)                     => s"sdel ${printData(key)}"
//      case ProgramCreate(owner, address, program) => ???
//      case ProgramUpdate(address, program)        => ???
//      case ProgramSeal(address)                   => ???
//      case BalanceGet(address, coins)             => s"balance x${byteUtils.byteString2hex(address)} $coins"
//      case BalanceAccrue(address, coins)          => ???
//      case BalanceWithdraw(address, coins)        => ???
//      case BalanceTransfer(from, to, coins) =>
//        s"transfer x${byteUtils.byteString2hex(from)} x${byteUtils.byteString2hex(to)} $coins"
//      // TODO implement printing of all other effects
//    }
//
//    def printEvent(event: EnviromentEvent) = event match {
//      case VmSandbox.EnviromentEvent(address, name, data) =>
//        s"x${byteUtils.byteString2hex(address)} $name ${data.mkString()}"
//    }
//
//    def combine(ops: Seq[(String, Option[String])]): String =
//      ops
//        .flatMap { op =>
//          for {
//            text <- op._2
//          } yield s"""|${op._1}:
//              ${text.split('\n').map("|  " + _).mkString("\n")}""".stripMargin
//        }
//        .mkString("\n")
//
//    def nonEmptyReduce[T, A[T] <: Iterable[T], B](a: A[T])(reduce: A[T] => B): Option[B] =
//      if (a.isEmpty) {
//        None
//      } else {
//        Some(reduce(a))
//      }
//
//    s"watts-spent: ${e.watts}\n" +
//      combine(
//        Seq(
//          "stack" -> nonEmptyReduce(e.memory.stack)(_.map(printData).mkString(", ")),
//          "heap" -> nonEmptyReduce(e.memory.heap.toSeq)(_.map { case (k, v) => s"${printData(k)} = ${printData(v)}" }
//            .mkString(",\n")),
//          "effects" -> nonEmptyReduce(e.effects)(_.map(printEffect).mkString(",\n")),
//          "events" -> nonEmptyReduce(e.events)(_.map(printEvent).mkString(",\n")),
//          "error" -> nonEmptyReduce(e.error.toList)(_.head.split('\n').map("|" + _).mkString("\n"))
//        ))
//  }
//
//  def sandboxRun(ops: Seq[Operation], pre: Preconditions): Expectations = {
//
//  }
}
