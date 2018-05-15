package io.mytc.sood.asm

sealed trait Op
sealed trait Datum

object Datum {
  final case class Integral(value: Int)    extends Datum { override def toString = value.toString }
  final case class Floating(value: Double) extends Datum { override def toString = value.toString }

  final case class Rawbytes(value: Array[Byte]) extends Datum {
    override def toString = s"Rawbytes(${value.mkString(", ")})"
    override def equals(other: Any): Boolean = other match {
      case Rawbytes(array) ⇒ this.value.deep == array.deep
      case _               ⇒ false
    }
  }
}

object Op {

  final case class Label(name: String) extends Op // { override def toString = "@" + name + ":" }
  case object Stop                     extends Op // { override def toString = "stop" }
  final case class Jump(name: String)  extends Op { override def toString = "jmp @" + name }
  final case class JumpI(name: String) extends Op { override def toString = "jmpi @" + name }
//
  case object Pop                 extends Op // { override def toString = "pop" }
  final case class Push(d: Datum) extends Op // { override def toString = "push " + d.toString }
  case object Dup                 extends Op // { override def toString = "dup" }
  case object Swap                extends Op // { override def toString = "swap" }
//
  final case class Call(name: String) extends Op // { override def toString = "call @" + name }
  case object Ret                     extends Op // { override def toString = "ret" }
  case object MPut                    extends Op // { override def toString = "mput" }
  case object MGet                    extends Op // { override def toString = "mget" }
//
  case object I32Add extends Op // { override def toString = "i32add" }
  case object I32Mul extends Op // { override def toString = "i32mul" }
  case object I32Div extends Op // { override def toString = "i32div" }
  case object I32Mod extends Op // { override def toString = "i32mod" }
//
  case object FAdd extends Op // { override def toString = "fadd" }
  case object FMul extends Op // { override def toString = "fmul" }
  case object FDiv extends Op // { override def toString = "fdiv" }
  case object FMod extends Op // { override def toString = "fmod" }

  case object Not extends Op
  case object I32LT extends Op
  case object I32GT extends Op

  case object Eq extends Op
//
  case object Nop extends Op // { override def toString = "nop" }

  case object Dupn extends Op

  final case class PCall(address: Array[Byte], argsNum: Int) extends Op
  final case class LCall(address: String, func: String, argsNum: Int) extends Op {
    //override def toString = s"lcall $adress $func $argsNum"
  }

  case object SGet extends Op
  case object SPut extends Op
}
