package io.mytc.sood.vm

import java.nio.ByteBuffer

import Vm.Data
import VmUtils._

case class Program(bytes: Vector[Byte] = Vector.empty[Byte], stack: Vector[Word] = Vector.empty[Word]) {
  def buffer: ByteBuffer = ByteBuffer.wrap(bytes.toArray)

  def put(i: Int): Program = {
    copy(bytes ++ int32ToWord(i))
  }
  def put(b: Byte): Program = {
    copy(bytes ++ bytesToWord(Array(b)))
  }
  def put(bs: Array[Byte]): Program = {
    copy(bytes ++ bytesToWord(bs))
  }

  def opcode(cmd: Int): Program = {
    copy(bytes :+ cmd.toByte)
  }
  def opcode(cmd: Byte): Program = {
    copy(bytes :+ cmd)
  }
  def withStack(stack: Data*): Program = copy(stack = stack.to[Vector])

  def length: Int = bytes.length
  def + (p: Program): Program = copy(bytes = bytes ++ p.bytes, stack = stack ++ p.stack)

  override def toString: String = {
    s"""
       |program: ${hex(bytes)}
       |init stack:
       |  ${stack.reverse.map(x => hex(x)).mkString("\n\t")}
     """.stripMargin
  }
}

