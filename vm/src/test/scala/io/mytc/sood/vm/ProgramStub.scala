package io.mytc.sood.vm

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import state.Data
import com.google.protobuf.ByteString

final case class ProgramStub(bytes: Vector[Byte] = Vector.empty[Byte], stack: Vector[Data] = Vector.empty[Data]) {
  lazy val buffer: ByteBuffer =
    ByteBuffer.wrap(stack.flatMap(w => Array(Opcodes.PUSHX) ++ bytesToWord(w.toByteArray)).toArray ++ bytes)

  lazy val byteString: Data =
    ByteString.copyFrom(buffer)

  def put(d: Double): ProgramStub = {
    copy(bytes ++ doubleToWord(d))
  }

  def put(i: Int): ProgramStub = {
    copy(bytes ++ int32ToWord(i))
  }
  def put(b: Byte): ProgramStub = {
    copy(bytes ++ bytesToWord(Array(b)))
  }
  def put(bs: Array[Byte]): ProgramStub = {
    copy(bytes ++ bytesToWord(bs))
  }
  def put(bs: ByteString): ProgramStub = {
    copy(bytes ++ bytesToWord(bs.toByteArray))
  }
  def put(bs: String): ProgramStub = {
    copy(bytes ++ bytesToWord(bs.getBytes(StandardCharsets.UTF_8)))
  }

  def opcode(cmd: Int): ProgramStub = {
    copy(bytes :+ cmd.toByte)
  }
  def opcode(cmd: Byte): ProgramStub = {
    copy(bytes :+ cmd)
  }
  def withStack(stack: Data*): ProgramStub = copy(stack = stack.to[Vector])

  def length: Int = bytes.length
  def + (p: ProgramStub): ProgramStub = copy(bytes = bytes ++ p.bytes, stack = stack ++ p.stack)

  // FIXME
//  override def toString: String = {
//    s"""
//       |program: ${hex(bytes)}
//       |init stack:
//       |  ${stack.reverse.map(x => x.toHex).mkString("\n\t")}
//     """.stripMargin
//  }
}

