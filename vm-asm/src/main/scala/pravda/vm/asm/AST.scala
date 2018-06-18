package pravda.vm.asm

import pravda.common.bytes
import pravda.vm.state.Data_

sealed trait Op {
  def toAsm: String
}

sealed trait Datum {
  def toAsm: String
}

object Datum {
  final case class Integral(value: Int)    extends Datum { override def toAsm = value.toString }
  final case class Floating(value: Double) extends Datum { override def toAsm = value.toString }

  final case class Rawbytes(value: Array[Byte]) extends Datum {
    override def toString = s"Rawbytes(${value.mkString(", ")})"
    override def equals(other: Any): Boolean = other match {
      case Rawbytes(array) => this.value sameElements array
      case _               => false
    }
    override def toAsm = bytes.bytes2hex(value)
  }
}

sealed trait MetaInfo {
  def toAsm: String
}

object MetaInfo {

  def tpeToString(tpe: Byte): String = tpe match {
    case Data_.TypeNull    => "null"
    case Data_.TypeInt8    => "int8"
    case Data_.TypeInt16   => "int16"
    case Data_.TypeInt32   => "int32"
    case Data_.TypeBigInt  => "bigInt"
    case Data_.TypeUint8   => "uint8"
    case Data_.TypeUint16  => "uint16"
    case Data_.TypeUint32  => "uint32"
    case Data_.TypeNumber  => "number"
    case Data_.TypeBoolean => "bool"
    case Data_.TypeRef     => "ref"
    case Data_.TypeUtf8    => "utf8"
    case Data_.TypeArray   => "array"
    case Data_.TypeStruct  => "struct"
  }

  final case class Method(name: String, returnTpe: Byte, argsTpes: List[Byte]) extends MetaInfo {
    override def toAsm: String =
      s"method $name [${tpeToString(returnTpe)}] [${argsTpes.map(tpeToString).mkString(", ")}]"
  }
}

object Op {

  final case class Label(name: String) extends Op { override def toAsm = "@" + name + ":" }
  case object Stop                     extends Op { override def toAsm = "stop" }
  final case class Jump(name: String)  extends Op { override def toAsm = "jmp @" + name }
  final case class JumpI(name: String) extends Op { override def toAsm = "jmpi @" + name }

  case object Pop                 extends Op { override def toAsm = "pop" }
  final case class Push(d: Datum) extends Op { override def toAsm = "push " + d.toAsm }
  case object Dup                 extends Op { override def toAsm = "dup" }
  case object Swap                extends Op { override def toAsm = "swap" }
  case object SwapN               extends Op { override def toAsm = "swapn" }

  final case class Call(name: String) extends Op { override def toAsm = "call @" + name }
  case object Ret                     extends Op { override def toAsm = "ret" }
  case object MPut                    extends Op { override def toAsm = "mput" }
  case object MGet                    extends Op { override def toAsm = "mget" }

  case object I32Add extends Op { override def toAsm = "i32add" }
  case object I32Mul extends Op { override def toAsm = "i32mul" }
  case object I32Div extends Op { override def toAsm = "i32div" }
  case object I32Mod extends Op { override def toAsm = "i32mod" }

  case object FAdd extends Op { override def toAsm = "fadd" }
  case object FMul extends Op { override def toAsm = "fmul" }
  case object FDiv extends Op { override def toAsm = "fdiv" }
  case object FMod extends Op { override def toAsm = "fmod" }

  case object Not   extends Op { override def toAsm = "not" }
  case object I32LT extends Op { override def toAsm = "i32lt" }
  case object I32GT extends Op { override def toAsm = "i32gt" }

  case object Eq  extends Op { override def toAsm = "eq" }
  case object Nop extends Op { override def toAsm = "nop" }

  case object Dupn    extends Op { override def toAsm = "dupn" }
  case object Concat  extends Op { override def toAsm = "concat" }
  case object From    extends Op { override def toAsm = "from" }
  case object PCreate extends Op { override def toAsm = "pcreate" }
  case object PUpdate extends Op { override def toAsm = "pupdate" }

  case object PCall extends Op { override def toAsm = "pcall" }

  final case class LCall(address: String, func: String, argsNum: Int) extends Op {
    override def toAsm = s"lcall $address $func $argsNum"
  }

  case object SGet  extends Op { override def toAsm = "sget" }
  case object SPut  extends Op { override def toAsm = "sput" }
  case object SExst extends Op { override def toAsm = "sexist" }

  case object Transfer extends Op { override def toAsm = "transfer" }

  final case class Meta(info: MetaInfo) extends Op { override def toAsm: String = s"meta $info" }

}
