package pravda.vm.asm

import pravda.vm.{Data, Opcodes}

sealed trait Operation

object Operation {

  final case class Comment(value: String)      extends Operation
  final case class Push(d: Data)               extends Operation
  final case class New(d: Data)                extends Operation
  final case class Label(name: String)         extends Operation
  final case class Jump(name: Option[String])  extends Operation
  final case class JumpI(name: Option[String]) extends Operation
  final case class Call(name: Option[String])  extends Operation
  final case class Meta(meta: pravda.vm.Meta)  extends Operation

  case object Pop      extends Operation
  case object Dup      extends Operation
  case object Swap     extends Operation
  case object Swapn    extends Operation
  case object Ret      extends Operation
  case object Add      extends Operation
  case object Mul      extends Operation
  case object Div      extends Operation
  case object Mod      extends Operation
  case object Not      extends Operation
  case object Lt       extends Operation
  case object Gt       extends Operation
  case object Eq       extends Operation
  case object Nop      extends Operation
  case object Dupn     extends Operation
  case object From     extends Operation
  case object PCreate  extends Operation
  case object PUpdate  extends Operation
  case object PCall    extends Operation
  case object LCall    extends Operation
  case object SGet     extends Operation
  case object SPut     extends Operation
  case object SExist   extends Operation
  case object Stop     extends Operation
  case object Transfer extends Operation

  val operationToCode: Map[Operation, Int] = Map(
    Pop      -> Opcodes.POP,
    Dup      -> Opcodes.DUP,
    Dupn     -> Opcodes.DUPN,
    Swap     -> Opcodes.SWAP,
    Swapn    -> Opcodes.SWAPN,
    Ret      -> Opcodes.RET,
    Add      -> Opcodes.ADD,
    Mul      -> Opcodes.MUL,
    Div      -> Opcodes.DIV,
    Mod      -> Opcodes.MOD,
    Not      -> Opcodes.NOT,
    Lt       -> Opcodes.LT,
    Gt       -> Opcodes.GT,
    Eq       -> Opcodes.EQ,
    From     -> Opcodes.FROM,
    PCreate  -> Opcodes.PCREATE,
    PUpdate  -> Opcodes.PUPDATE,
    PCall    -> Opcodes.PCALL,
    LCall    -> Opcodes.LCALL,
    SGet     -> Opcodes.SGET,
    SPut     -> Opcodes.SPUT,
    SExist   -> Opcodes.SEXIST,
    Stop     -> Opcodes.STOP,
    Transfer -> Opcodes.TRANSFER
  )

  val codeToOperation: Map[Int, Operation] = operationToCode map {
    case (k, v) =>
      (v, k)
  }

  val SimpleOperations: Set[Operation] =
    operationToCode.keys.toSet
}
