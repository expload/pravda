package io.mytc.sood.vm

import org.scalatest.{FlatSpec, Matchers}
import VmUtils._
import Opcodes._
import io.mytc.sood.vm.state.VmError._
import io.mytc.sood.vm.state.{VmError, VmErrorException, WorldState}

class ErrorSpec extends FlatSpec with Matchers {

  final case class ErrorTestWithState(program: Program, state: WorldState) {
    def shouldOccurError(error: VmError): Unit = {
      an [VmErrorException] should be thrownBy exec(program, state)
      val thrown = the [VmErrorException] thrownBy exec(program, state)
      thrown.error shouldBe error
    }
  }

  implicit class ErrorTest(program: Program) {
    def shouldOccurError(error: VmError): Unit = {
      an [VmErrorException] should be thrownBy exec(program)
      val thrown = the [VmErrorException] thrownBy exec(program)
      thrown.error shouldBe error
    }

    def withState(worldState: WorldState): ErrorTestWithState = ErrorTestWithState(program, worldState)

  }


  "StackUnderflow" should "occur when popping an empty stack" in {
    val program = prog.opcode(POP)
    program shouldOccurError StackUnderflow


    val program2 = prog.opcode(PUSHX).put(1).opcode(POP).opcode(POP)
    program2 shouldOccurError StackUnderflow

  }

  it should "occur when calling function with too many arguments" in {

    val address = binaryData(13, 15, 66, 78)
    val storedProg = prog.opcode(PUSHX).put(111)

    val wState = worldState(address -> storedProg)

    val program1 = prog.opcode(PCALL).put(address).put(1)
    program1 withState wState shouldOccurError StackUnderflow

    val program2 = prog.opcode(PUSHX).put(4).opcode(PCALL).put(address).put(2)
    program2 withState wState shouldOccurError StackUnderflow

  }

  "NoSuchProgram" should "occur when colling not existent program" in {
    val address = binaryData(13, 15, 66, 78)
    val wrongAddress = binaryData(13, 15, 0, 78)

    val storedProg = prog.opcode(PUSHX).put(111)

    val wState = worldState(address -> storedProg)

    val program1 = prog.opcode(PCALL).put(wrongAddress).put(0)
    program1 withState wState shouldOccurError NoSuchProgram

  }

  "Error" should "occur when remote program fails" in {
    val address = binaryData(13, 15, 66, 78)

    val storedProg = prog.opcode(POP)

    val wState = worldState(address -> storedProg)

    val program1 = prog.opcode(PCALL).put(address).put(0)
    program1 withState wState shouldOccurError StackUnderflow

  }

    "NoSuchLibrary" should "occur when colling not existent library" in {
    val address = binaryData(13, 15, 66, 78)
    val wrongAddress = binaryData(13, 15, 0, 78)

    val storedProg = prog.opcode(PUSHX).put(111)

    val wState = worldState(address -> storedProg)

    val program1 = prog.opcode(LCALL).put(wrongAddress).put("mypush").put(0)
    program1 withState wState shouldOccurError NoSuchLibrary

  }

  it should "occur when it is not a library" in {
    val address = binaryData(13, 15, 66, 78)

    val regularProgram = prog.opcode(PUSHX).put(111)

    val wState = worldState(address -> regularProgram)

    val program1 = prog.opcode(LCALL).put(address).put("mypush").put(0)
    program1 withState wState shouldOccurError NoSuchLibrary

  }

  "NoSuchMethod" should "occur when there is no such method in the library" in {

    val address = binaryData(13, 15, 66, 78)

    val libraryMethod = prog.opcode(FTBL).put(1).put("meth1").put(10)

    val wState = worldState(address -> libraryMethod)

    val program1 = prog.opcode(LCALL).put(address).put("meth2").put(0)
    program1 withState wState shouldOccurError NoSuchMethod

  }

  "OperationDenied" should "occur when one uses storage from transaction" in {

    val program1 = prog.opcode(SGET).put(0)
    program1 shouldOccurError OperationDenied

  }

}
