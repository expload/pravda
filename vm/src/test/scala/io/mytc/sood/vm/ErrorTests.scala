package io.mytc.sood.vm

import utest.TestSuite
import utest._
import VmUtils._
import Opcodes._
import io.mytc.sood.vm.state.VmError._
import io.mytc.sood.vm.state.{Environment, VmError, VmErrorException}

object ErrorTests extends TestSuite {

  val tests = Tests {

    def assertError(program: ProgramStub, error: VmError, env: Environment = emptyState): Unit = {
      val e = intercept[VmErrorException] {
        exec(program, env)
      }
      assert(e.error == error)
    }

    'stackUnderflow - {

      'simple - {

        val program = prog.opcode(POP)
        assertError(program, StackUnderflow)

        val program2 = prog.opcode(PUSHX).put(1).opcode(POP).opcode(POP)
        assertError(program2, StackUnderflow)

      }

      'complex - {

        val address = binaryData(13, 15, 66, 78)
        val storedProg = prog.opcode(PUSHX).put(111)

        val wState = environment(address -> storedProg)

        val program1 = prog.opcode(PCALL).put(address).put(1)
        assertError(program1, StackUnderflow, env = wState)

        val program2 = prog.opcode(PUSHX).put(4).opcode(PCALL).put(address).put(2)
        assertError(program2, StackUnderflow, env = wState)

      }

    }

    'noSuchProgram - {

      val address = binaryData(13, 15, 66, 78)
      val wrongAddress = binaryData(13, 15, 0, 78)

      val storedProg = prog.opcode(PUSHX).put(111)

      val wState = environment(address -> storedProg)

      val program1 = prog.opcode(PUSHX).put(wrongAddress).opcode(PUSHX).put(0).opcode(PCALL)
      assertError(program1, NoSuchProgram, env = wState)

    }

    'noSuchLibrary - {

      'notExists - {
        val address = binaryData(13, 15, 66, 78)
        val wrongAddress = binaryData(13, 15, 0, 78)

        val storedProg = prog.opcode(PUSHX).put(111)

        val wState = environment(address -> storedProg)

        val program1 = prog.opcode(LCALL).put(wrongAddress).put("mypush").put(0)
        assertError(program1, NoSuchLibrary, env = wState)

      }

      'notLibrary - {

        val address = binaryData(13, 15, 66, 78)

        val regularProgram = prog.opcode(PUSHX).put(111)

        val wState = environment(address -> regularProgram)

        val program1 = prog.opcode(LCALL).put(address).put("mypush").put(0)
        assertError(program1, NoSuchLibrary, env = wState)

      }

    }

    'externalError - {
        val address = binaryData(13, 15, 66, 78)

        val storedProg = prog.opcode(POP)

        val wState = environment(address -> storedProg)

        val program1 = prog.opcode(PCALL).put(address).put(0)
        assertError(program1, StackUnderflow, env = wState)

      }


      'noSuchMethod - {

        val address = binaryData(13, 15, 66, 78)

        val libraryMethod = prog.opcode(FTBL).put(1).put("meth1").put(10)

        val wState = environment(address -> libraryMethod)

        val program1 = prog.opcode(LCALL).put(address).put("meth2").put(0)
        assertError(program1, NoSuchMethod, env = wState)

      }

      'operationDenied - {

        val program1 = prog.opcode(SGET).put(0)
        assertError(program1, OperationDenied)

      }
  }
}
