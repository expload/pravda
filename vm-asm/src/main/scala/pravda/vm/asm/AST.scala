package pravda.vm.asm

sealed abstract class Op(val toAsm: String)
sealed abstract class Datum(val toAsm: String)

object Datum {

  final case class Integral(value: Int)    extends Datum(value.toString)
  final case class Floating(value: Double) extends Datum(value.toString)

  final case class Rawbytes(value: Array[Byte]) extends Datum(value.map("%02X".format(_)).mkString(" ")) {
    override def toString = s"Rawbytes(${value.mkString(", ")})"
    override def equals(other: Any): Boolean = other match {
      case Rawbytes(array) => this.value sameElements array
      case _               => false
    }
  }
}

object Op {

  final case class Label(name: String) extends Op("@" + name + ":")

  case object Stop                     extends Op("stop")
  final case class Jump(name: String)  extends Op("jmp @" + name)
  final case class JumpI(name: String) extends Op("jmpi @" + name)

  case object Pop                 extends Op("pop")
  final case class Push(d: Datum) extends Op("push " + d.toAsm)
  case object Dup                 extends Op("dup")
  case object Swap                extends Op("swap")
  case object SwapN               extends Op("swapn")

  final case class Call(name: String) extends Op("call @" + name)
  case object Ret                     extends Op("ret")
  case object MPut                    extends Op("mput")
  case object MGet                    extends Op("mget")

  case object Add extends Op("add")
  case object Mul extends Op("mul")
  case object Div extends Op("div")
  case object Mod extends Op("mod")

  case object Not   extends Op("not")
  case object Lt extends Op("сlt")
  case object Gt extends Op("сgt")

  case object Eq  extends Op("eq")
  case object Nop extends Op("nop")

  case object Dupn    extends Op("dupn")
  case object From    extends Op("from")
  case object PCreate extends Op("pcreate")
  case object PUpdate extends Op("pupdate")

  case object PCall extends Op("pcall")
  final object LCall extends Op(s"lcall")

  case object SGet  extends Op("sget")
  case object SPut  extends Op("sput")
  case object SExst extends Op("sexist")

  case object Transfer extends Op("transfer")
}
