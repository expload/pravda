package io.mytc.sood.asm

sealed trait Op

object Op {

  case class  Label(name: String) extends Op { override def toString = "@" + name + ":" }
  case object Stop                extends Op { override def toString = "stop" }
  case object Jump                extends Op { override def toString = "jmp" }
  case object JumpI               extends Op { override def toString = "jmpi" }

  case object Pop                 extends Op { override def toString = "pop" }
  case class  Push(x: Int)        extends Op { override def toString = "push " + x.toString }
  case object Dup                 extends Op { override def toString = "dup" }
  case object Swap                extends Op { override def toString = "swap" }

  case class  Call(name: String)  extends Op { override def toString = "call @" + name }
  case object Ret                 extends Op { override def toString = "ret" }
  case object MPut                extends Op { override def toString = "mput" }
  case object MGet                extends Op { override def toString = "mget" }

  case object I32Add              extends Op { override def toString = "i32add" }
  case object I32Mul              extends Op { override def toString = "i32mul" }
  case object I32Div              extends Op { override def toString = "i32div" }
  case object I32Mod              extends Op { override def toString = "i32mod" }

  case object Nop                 extends Op { override def toString = "nop" }

}
