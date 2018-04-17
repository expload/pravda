package io.mytc.sood.vm
package opcode

import java.nio.ByteBuffer
import java.util

import org.scalatest.{FlatSpec, Matchers}
import VmUtils._
import Opcodes.int._
import io.mytc.sood.vm.state.{AccountState, Address, Storage, WorldState}


class CallSpec extends FlatSpec with Matchers {

  val address1 = bytes(4, 5, 66, 78)
  val address2 = bytes(43, 15, 66, 78)
  val address3 = bytes(13, 15, 66, 78)


  def worldState(accs: (Address, Program)*) = new WorldState {

    def account(prog: Program): AccountState = new AccountState {
      override def program: ByteBuffer = prog.buffer
      override def storage: Storage = null
    }

    override def get(address: Address): AccountState = {
      accs.find{case (addr, _) => util.Arrays.equals(addr, address)}.map{
        case (addr, prog) => account(prog)
      }.get
    }
  }


  "PCALL opcode" should "execute the program with the sepecifiedd address" in {

    val prog1 = prog.opcode(PUSHX).put(111)
    val prog2 = prog.opcode(PUSHX).put(222)
    val sum = prog.opcode(I32ADD)

    val wState = worldState(address1 -> prog1, address2 -> prog2, address3 -> sum)

    val programA1 = prog.opcode(PUSHX).put(address1).opcode(PCALL)
    exec(programA1, wState) shouldBe stack(data(111))
    val programA2 = prog.opcode(PUSHX).put(333).opcode(PUSHX).put(address2).opcode(PCALL).opcode(I32ADD)
    exec(programA2, wState) shouldBe stack(data(555))

    val programSum = prog.opcode(PUSHX).put(7).opcode(PUSHX).put(13).opcode(PUSHX).put(address3).opcode(PCALL)
    exec(programSum, wState) shouldBe stack(data(20))

  }
}
