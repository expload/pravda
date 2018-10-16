/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.vm.asm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import fastparse.parsers.Combinators.Rule
import pravda.ParsingCommons
import pravda.ParsingCommons.ParsingError
import pravda.vm.{Data, Opcodes, Meta => Metadata}

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
  import Operation.{mnemonicByOpcode => mnemonic, _}

  /**
    * Parses text and assemble to Pravda VM bytecode.
    * @param saveLabels Add META opcodes with infomation about labels.
    *                   It can be used by disassembler.
    * @return Error | Bytecode
    */
  def assemble(text: String, saveLabels: Boolean): Either[ParsingError, ByteString] =
    parse(text).map(assemble(_, saveLabels))

  /**
    * Generates Pavda VM compatible bytecode from sequence of operations.
    * You can obtain sequence of operations using [[parser]].
    * @param saveLabels Add META opcodes with infomation about labels.
    *                   It can be used by disassembler.
    */
  def assemble(operations: Seq[Operation], saveLabels: Boolean): ByteString = {
    val labels = mutable.Map.empty[String, Int] // label -> offset
    val gotos = mutable.Map.empty[Int, String] // offset -> label
    val bytecode = ByteBuffer.allocate(1024 * 1024)

    def putOp(opcode: Int): Unit =
      bytecode.put(opcode.toByte)

    // put placeholder for the label reference
    def pushRef(name: String): Unit = {
      if (saveLabels) {
        putOp(Opcodes.META)
        Metadata.LabelUse(name).writeToByteBuffer(bytecode)
      }
      putOp(Opcodes.PUSHX)
      // Save goto offset to set in a future
      gotos.put(bytecode.position, name)
      // Just a placeholer. Ref is constant sized
      Primitive.Ref.Void.writeToByteBuffer(bytecode)
    }

    // Go to `name` using `opcode`
    def goto(name: String, opcode: Int): Unit = {
      pushRef(name)
      putOp(opcode)
    }

    for (operation <- operations if operation != Nop) operation match {
      case StructGet(Some(key)) =>
        putOp(Opcodes.STRUCT_GET_STATIC)
        key.writeToByteBuffer(bytecode)
      case StructGet(None) =>
        putOp(Opcodes.STRUCT_GET)
      case StructMut(None) =>
        putOp(Opcodes.STRUCT_MUT)
      case StructMut(Some(key)) =>
        putOp(Opcodes.STRUCT_MUT_STATIC)
        key.writeToByteBuffer(bytecode)
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
      case Meta(data) =>
        putOp(Opcodes.META)
        data.writeToByteBuffer(bytecode)
      case Jump(Some(name))  => goto(name, Opcodes.JUMP)
      case JumpI(Some(name)) => goto(name, Opcodes.JUMPI)
      case Call(Some(name))  => goto(name, Opcodes.CALL)
      case Jump(None)        => putOp(Opcodes.JUMP)
      case JumpI(None)       => putOp(Opcodes.JUMPI)
      case Call(None)        => putOp(Opcodes.CALL)
      case PushRef(name)     => pushRef(name)
      case Orphan(opcode)    => putOp(opcode)
      // Simple operations
      case Nop        => ()
      case _: Comment => ()
    }

    // Size will be not changed anymore
    bytecode.flip()

    for ((offset, label) <- gotos) {
      val data = Primitive.Ref(labels(label))
      // Replace placeholder with real offset
      bytecode.position(offset)
      data.writeToByteBuffer(bytecode)
    }

    bytecode.rewind()
    ByteString.copyFrom(bytecode)
  }

  /**
    * Disassembles bytecode to sequence of operations.
    * Use render() to build text from sequence of operations.
    */
  def disassemble(bytecode: ByteString): Seq[(Int, Operation)] = {
    val buffer = bytecode.asReadOnlyByteBuffer()
    val operations = mutable.Buffer.empty[(Int, Operation)]
    var label = Option.empty[String]

    while (buffer.hasRemaining) {
      val offset = buffer.position()
      val opcode = buffer.get & 0xff

      (opcode: @switch) match {
        case Opcodes.META =>
          Metadata.readFromByteBuffer(buffer) match {
            case Metadata.LabelDef(name) =>
              operations += (offset -> Operation.Label(name))
            case Metadata.LabelUse(name) =>
              label = Some(name)
            case metadata =>
              operations += (offset -> Operation.Meta(metadata))
          }
        case Opcodes.STRUCT_GET_STATIC =>
          val offset = buffer.position()
          Data.readFromByteBuffer(buffer) match {
            case key: Primitive => operations += (offset -> StructGet(Some(key)))
            case data           => throw UnexpectedTypeException(data, Some(offset))
          }
        case Opcodes.STRUCT_MUT_STATIC =>
          val offset = buffer.position()
          Data.readFromByteBuffer(buffer) match {
            case key: Primitive => operations += (offset -> StructMut(Some(key)))
            case data           => throw UnexpectedTypeException(data, Some(offset))
          }
        case Opcodes.PUSHX if label.nonEmpty =>
          Data.readFromByteBuffer(buffer)
          operations += (offset -> Operation.PushRef(label.get))
          label = None
        case Opcodes.PUSHX =>
          val offset = buffer.position()
          Data.readFromByteBuffer(buffer) match {
            case p: Primitive => operations += (offset -> Operation.Push(p))
            case data         => throw UnexpectedTypeException(data, Some(offset))
          }
        case Opcodes.JUMP =>
          operations.last match {
            case (_, Operation.PushRef(l)) =>
              operations.remove(operations.length - 1)
              operations += (offset -> Operation.Jump(Some(l)))
            case _ =>
              operations += (offset -> Operation.Jump(None))
          }
        case Opcodes.JUMPI =>
          operations.last match {
            case (_, Operation.PushRef(l)) =>
              operations.remove(operations.length - 1)
              operations += (offset -> Operation.JumpI(Some(l)))
            case _ =>
              operations += (offset -> Operation.JumpI(None))
          }
        case Opcodes.CALL =>
          operations.last match {
            case (_, Operation.PushRef(l)) =>
              operations.remove(operations.length - 1)
              operations += (offset -> Operation.Call(Some(l)))
            case _ =>
              operations += (offset -> Operation.Call(None))
          }

        case Opcodes.NEW =>
          operations += (offset -> Operation.New(Data.readFromByteBuffer(buffer)))
        case Opcodes.STRUCT_GET =>
          operations += (offset -> Operation.StructGet(None))
        case Opcodes.STRUCT_MUT =>
          operations += (offset -> Operation.StructMut(None))
        case op if Operation.operationByOpcode.contains(op) =>
          operations += (offset -> Operation.operationByOpcode(op))
      }
    }

    operations.filter(_._2 != Nop)
  }

  /**
    * Prints one operation to string.
    * @param pretty uses line breaks for structs,
    *               adds space after commas and colons, etc.
    */
  def render(operation: Operation, pretty: Boolean): String = operation match {
    case Jump(Some(label))  => s"${mnemonic(Opcodes.JUMP)} @$label"
    case JumpI(Some(label)) => s"${mnemonic(Opcodes.JUMPI)} @$label"
    case Call(Some(label))  => s"${mnemonic(Opcodes.CALL)} @$label"
    case PushRef(label)     => s"${mnemonic(Opcodes.PUSHX)} @$label"
    case Jump(None)         => mnemonic(Opcodes.JUMP)
    case JumpI(None)        => mnemonic(Opcodes.JUMPI)
    case Call(None)         => mnemonic(Opcodes.CALL)
    case Comment(value)     => s"/*$value*/"
    case Push(data)         => s"${mnemonic(Opcodes.PUSHX)} ${data.mkString(pretty = pretty)}"
    case New(data)          => s"${mnemonic(Opcodes.NEW)} ${data.mkString(pretty = pretty)}"
    case Label(name)        => s"@$name:"
    case StructGet(Some(k)) => s"${mnemonic(Opcodes.STRUCT_GET)} ${k.mkString(pretty = pretty)}"
    case StructMut(Some(k)) => s"${mnemonic(Opcodes.STRUCT_MUT)} ${k.mkString(pretty = pretty)}"
    case StructGet(None)    => mnemonic(Opcodes.STRUCT_GET)
    case StructMut(None)    => mnemonic(Opcodes.STRUCT_MUT)
    case Orphan(opcode)     => mnemonic(opcode)
    case Nop                => ""
    case Meta(info)         => s"${mnemonic(Opcodes.META)} ${info.mkString}"
  }

  /**
    * Pretty prints sequence of operation to string.
    * Received text is valid assembler text
    * which may be parsed by [[parser]].
    */
  def render(operations: Seq[Operation]): String =
    operations.map(render(_, pretty = true)).mkString("\n")

  /**
    * Parses text to sequence of operation.
    * @return Error | Operations
    */
  def parse(text: String): Either[ParsingError, Seq[Operation]] = {
    import fastparse.all._
    val p = P(Start ~ parser ~ End).parse(text)
    ParsingCommons.prettyPrintError(text, p)
  }

  val operationParser: fastparse.all.Parser[Operation] = {
    import fastparse.all._

    import Data.parser.{primitive => dataPrimitive, all => dataAll}
    val digit = P(CharIn('0' to '9'))
    val alpha = P(CharIn('a' to 'z', 'A' to 'Z', "_"))
    val alphadigdot = P(alpha | digit | ".")
    val ident = P("@" ~ (alpha.rep(1) ~ alphadigdot.rep(1).?).!)
    val delim = P(CharIn(" \t\r\n").rep(min = 1))

    val label = P(ident ~ ":").map(n => Operation.Label(n))
    val pushRef = P(IgnoreCase("push") ~ delim ~ ident).map(Operation.PushRef)
    val push = P(IgnoreCase("push") ~ delim ~ dataPrimitive).map(Operation.Push)
    val `new` = P(IgnoreCase("new") ~ delim ~ dataAll).map(Operation.New)
    val jump = P(IgnoreCase("jump") ~ (delim ~ ident).?).map(Operation.Jump)
    val jumpi = P(IgnoreCase("jumpi") ~ (delim ~ ident).?).map(Operation.JumpI)
    val call = P(IgnoreCase("call") ~ (delim ~ ident).?).map(Operation.Call)
    val struct_get = P(IgnoreCase("struct_get") ~ (delim ~ dataPrimitive).?).map(Operation.StructGet)
    val struct_mut = P(IgnoreCase("struct_mut") ~ (delim ~ dataPrimitive).?).map(Operation.StructMut)
    val comment = P("/*" ~ (!"*/" ~ AnyChar).rep.! ~ "*/").map(Operation.Comment)

    val meta = P("meta " ~ pravda.vm.Meta.parser.meta).map(Meta)

    Operation.Orphans
      .map(op => Rule(mnemonic(op.opcode), () => IgnoreCase(mnemonic(op.opcode))).map(_ => op))
      .++(Seq(jumpi, jump, call, pushRef, push, `new`, struct_get, struct_mut, label, comment, meta))
      .reduce(_ | _)
  }

  /**
    * Use this to parse assembler text
    * to sequence of operations.
    */
  val parser: fastparse.all.Parser[Seq[Operation]] = {
    import fastparse.all._

    val whitespace = P(CharIn(" \t\r\n").rep)
    val delim = P(CharIn(" \t\r\n").rep(min = 1))

    P(whitespace ~ operationParser.rep(sep = delim) ~ whitespace)
  }
}
