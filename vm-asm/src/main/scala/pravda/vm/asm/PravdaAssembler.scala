package pravda.vm.asm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import fastparse.all
import pravda.vm.{Data, Meta => Metadata, Opcodes}

import scala.annotation.switch
import scala.collection.mutable

/**
  * Low-level utility to work with Pravda VM bytecode.
  * [[PravdaAssembler]] operates with sequences of [[Operation]].
  * It can generate bytecode representation and
  * text representation of operations. Also it can
  * disassemble bytecode to sequence of [[Operation]].
  */
object PravdaAssembler {

  import Data._
  import Operation._

  /**
    * Generates Pavda VM compatible bytecode from sequence of operations.
    * You can obtain sequence of operations using [[parser]].
    * @param saveLabels Add META opcodes with infomation about labels. It can be used by disassembler.
    */
  def assemble(operations: Seq[Operation], saveLabels: Boolean = false): ByteString = {
    val labels = mutable.Map.empty[String, Int] // label -> offset
    val bytecode = ByteBuffer.allocate(1024 * 1024)

    def putOp(opcode: Int): Unit =
      bytecode.put(opcode.toByte)

    /** Go to `name` using `opcode` */
    def goto(name: String, opcode: Int): Unit = {
      val data = Primitive.Int32(labels(name))
      if (saveLabels) {
        putOp(Opcodes.META)
        Metadata.LabelUse(name).writeToByteBuffer(bytecode)
      }
      putOp(Opcodes.PUSHX)
      data.writeToByteBuffer(bytecode)
      putOp(opcode)
    }

    for (operation <- operations) operation match {
      // Complex operations
      case Push(data) =>
        putOp(Opcodes.PUSHX)
        data.writeToByteBuffer(bytecode)
      case New(data) =>
        putOp(Opcodes.NEW)
        data.writeToByteBuffer(bytecode)
      // Control
      case Label(name) =>
        if (saveLabels) {
          putOp(Opcodes.META)
          Metadata.LabelDef(name).writeToByteBuffer(bytecode)
        }
        labels.put(name, bytecode.position)
      case Jump(Some(name))  => goto(name, Opcodes.JUMP)
      case JumpI(Some(name)) => goto(name, Opcodes.JUMPI)
      case Call(Some(name))  => goto(name, Opcodes.CALL)
      case Jump(None)        => putOp(Opcodes.JUMP)
      case JumpI(None)       => putOp(Opcodes.JUMPI)
      case Call(None)        => putOp(Opcodes.CALL)
      case _: Comment        => ()
      // Simple operations
      case Nop => ()
      case _   => putOp(Operation.operationToCode(operation))
    }

    bytecode.flip()
    ByteString.copyFrom(bytecode)
  }

  /**
    * Disassembles bytecode to sequence of operations.
    */
  def disassemble(bytecode: ByteString): Seq[Operation] = {
    val buffer = bytecode.asReadOnlyByteBuffer()
    val operations = mutable.Buffer.empty[Operation]
    var label = Option.empty[String]
    while (buffer.hasRemaining) {
      val opcode = buffer.get & 0xff
      operations += Operation.codeToOperation.getOrElse(
        key = opcode,
        default = (opcode: @switch) match {
          case Opcodes.META =>
            Metadata.readFromByteBuffer(buffer) match {
              case Metadata.LabelDef(name) => Operation.Label(name)
              case Metadata.LabelUse(name) =>
                label = Some(name)
                Operation.Nop
              case metadata => Operation.Meta(metadata)
            }
          case Opcodes.PUSHX if label.nonEmpty =>
            Data.readFromByteBuffer(buffer)
            Operation.Nop
          case Opcodes.JUMP if label.nonEmpty =>
            val op = Operation.Jump(label)
            label = None
            op
          case Opcodes.JUMPI if label.nonEmpty =>
            val op = Operation.JumpI(label)
            label = None
            op
          case Opcodes.CALL if label.nonEmpty =>
            val op = Operation.Call(label)
            label = None
            op
          case Opcodes.NEW   => Operation.New(Data.readFromByteBuffer(buffer))
          case Opcodes.PUSHX => Operation.Push(Data.readFromByteBuffer(buffer))
          case Opcodes.JUMP  => Operation.Jump(None)
          case Opcodes.JUMPI => Operation.JumpI(None)
          case Opcodes.CALL  => Operation.Call(None)
        }
      )
    }

    operations.filter(_ != Nop)
  }

  /**
    * Prints one operation to string.
    */
  def render(operation: Operation): String = operation match {
    case Jump(Some(label))  => s"jump @$label"
    case JumpI(Some(label)) => s"jumpi @$label"
    case Call(Some(label))  => s"call @$label"
    case Comment(value)     => s"/*$value*/"
    case Push(data)         => s"push ${data.mkString(pretty = true)}"
    case New(data)          => s"new ${data.mkString(pretty = true)}"
    case Label(name)        => s"@$name:"
    case Jump(None)         => "jump"
    case JumpI(None)        => "jumpi"
    case Call(None)         => "call"
    // Simple
    case Pop      => "pop"
    case Dup      => "dup"
    case Dupn     => "dupn"
    case Swap     => "swap"
    case Swapn    => "swapn"
    case Ret      => "ret"
    case Add      => "add"
    case Mul      => "mul"
    case Div      => "div"
    case Mod      => "mod"
    case Not      => "not"
    case Lt       => "lt"
    case Gt       => "gt"
    case Eq       => "eq"
    case From     => "from"
    case PCreate  => "pcreate"
    case PUpdate  => "pupdate"
    case PCall    => "pcall"
    case LCall    => "lcall"
    case SGet     => "sget"
    case SPut     => "sput"
    case SExist   => "sexist"
    case Stop     => "stop"
    case Transfer => "transfer"
    case Meta(_)  => ""
    case Nop      => ""
  }

  /**
    * Prints sequence of operation to string.
    * Received text is valid assembler text
    * which may be parsed by [[parser]].
    */
  def render(operations: Seq[Operation]): String =
    operations.map(render).mkString("\n")

  /**
    * Use this to parse assembler text
    * to sequence of operations.
    */
  val parser: all.Parser[Seq[Operation]] = {

    import fastparse.all._

    val digit = P(CharIn('0' to '9'))
    val alpha = P(CharIn('a' to 'z', 'A' to 'Z'))
    val alphadig = P(alpha | digit)
    val ident = P("@" ~ (alpha.rep(1) ~ alphadig.rep(1).?).!)
    val delim = P(CharIn(" \t\r\n").rep(min = 1))

    val label = P(ident ~ ":").map(n => Operation.Label(n))
    val push = P(IgnoreCase("push") ~ delim ~ Data.parser.primitive).map(x => Operation.Push(x))
    val `new` = P(IgnoreCase("new") ~ delim ~ Data.parser.all).map(x => Operation.New(x))
    val jump = P(IgnoreCase("jump") ~ (delim ~ ident).?).map(n => Operation.Jump(n))
    val jumpi = P(IgnoreCase("jumpi") ~ (delim ~ ident).?).map(n => Operation.JumpI(n))
    val call = P(IgnoreCase("call") ~ (delim ~ ident).?).map(n => Operation.Call(n))

    def op(mnemocode: String, operation: Operation) =
      P(IgnoreCase(mnemocode)).map(_ => operation)

    val dup = op("dup", Operation.Dup)
    val dupn = op("dupn", Operation.Dupn)
    val swap = op("swap", Operation.Swap)
    val swapn = op("swapn", Operation.Swapn)
    val stop = op("stop", Operation.Stop)
    val ret = op("ret", Operation.Ret)
    val pop = op("pop", Operation.Pop)
    val add = op("add", Operation.Add)
    val mul = op("mul", Operation.Mul)
    val div = op("div", Operation.Div)
    val mod = op("mod", Operation.Mod)
    val lt = op("lt", Operation.Lt)
    val gt = op("gt", Operation.Gt)
    val eq = op("eq", Operation.Eq)
    val not = op("not", Operation.Not)
    val from = op("from", Operation.From)
    val pcreate = op("pcreate", Operation.PCreate)
    val pupdate = op("pupdate", Operation.PUpdate)
    val lcall = op("lcall", Operation.LCall)
    val pcall = op("pcall", Operation.PCall)
    val transfer = op("transfer", Operation.Transfer)
    val sexist = op("sexist", Operation.SExist)
    val sget = op("sget", Operation.SGet)
    val sput = op("sput", Operation.SPut)

    val comment = P("/*" ~ (!"*/" ~ AnyChar).rep.! ~ "*/")
      .map(s => Operation.Comment(s))

    val operation: P[Operation] = P(
      jumpi | jump | call
        | pop | push | dupn | dup | swapn | swap | add
        | mul | div | mod | lt | gt | eq | not
        | from | pcreate | pupdate | pcall | lcall
        | transfer | `new` | ret | sexist
        | sget | sput | stop | label | comment)

    P(Start ~ operation.rep(sep = delim) ~ End)
  }
}
