package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._
import utest._

import serialization._


object MathOpcodesTests extends TestSuite {

  val tests = Tests {
    'i32Add - {

      'sum - {

        val program = prog.withStack(int32ToData(2), int32ToData(3)).opcode(ADD)
        stackOfExec(program) ==> stack(int32ToData(5))

        // More bytes in stack
        val program2 = prog.withStack(int32ToData(77), int32ToData(880), int32ToData(13), int32ToData(24))
        stackOfExec(program2.opcode(ADD)) ==> stack(int32ToData(77), int32ToData(880), int32ToData(37))

      }

      'bigNumbers - {

        stackOfExec(prog.withStack(int32ToData(257), int32ToData(258)).opcode(ADD)).last ==> int32ToData(515)
        stackOfExec(prog.withStack(int32ToData(32534), int32ToData(32535)).opcode(ADD)).last ==> int32ToData(65069)
        stackOfExec(prog.withStack(int32ToData(65535), int32ToData(65534)).opcode(ADD)).last ==> int32ToData(131069)
        stackOfExec(prog.withStack(int32ToData(1073741823), int32ToData(1073741822)).opcode(ADD)).last ==> int32ToData(2147483645)

      }

      'negativeNumbers - {

        stackOfExec(prog.withStack(int32ToData(2), int32ToData(-3)).opcode(ADD)) ==> stack(int32ToData(-1))
        stackOfExec(prog.withStack(int32ToData(-2), int32ToData(-3)).opcode(ADD)) ==> stack(int32ToData(-5))
        stackOfExec(prog.withStack(int32ToData(-2), int32ToData(3)).opcode(ADD)) ==> stack(int32ToData(1))

      }

      'zero - {

        stackOfExec(prog.withStack(int32ToData(-2), int32ToData(0)).opcode(ADD)) ==> stack(int32ToData(-2))
        stackOfExec(prog.withStack(int32ToData(0), int32ToData(0)).opcode(ADD)) ==> stack(int32ToData(0))

      }
    }

    'i32mul - {
      stackOfExec(prog.withStack(int32ToData(-3), int32ToData(3)).opcode(MUL)) ==> stack(int32ToData(-9))
      stackOfExec(prog.withStack(int32ToData(123), int32ToData(-3), int32ToData(3)).opcode(MUL)) ==> stack(int32ToData(123), int32ToData(-9))
      stackOfExec(prog.withStack(int32ToData(0), int32ToData(3)).opcode(MUL)) ==> stack(int32ToData(0))
      stackOfExec(prog.withStack(int32ToData(-3), int32ToData(0)).opcode(MUL)) ==> stack(int32ToData(0))
    }

    'i32div - {
      stackOfExec(prog.withStack(int32ToData(-3), int32ToData(3)).opcode(DIV)) ==> stack(int32ToData(-1))
      stackOfExec(prog.withStack(int32ToData(123), int32ToData(-3), int32ToData(3)).opcode(DIV)) ==> stack(int32ToData(123), int32ToData(-1))
      stackOfExec(prog.withStack(int32ToData(2), int32ToData(0)).opcode(DIV)) ==> stack(int32ToData(0))
      stackOfExec(prog.withStack(int32ToData(2), int32ToData(-5)).opcode(DIV)) ==> stack(int32ToData(-2))
      stackOfExec(prog.withStack(int32ToData(2), int32ToData(5)).opcode(DIV)) ==> stack(int32ToData(2))
    }

    'i32mod - {

      stackOfExec(prog.withStack(int32ToData(-3), int32ToData(3)).opcode(MOD)) ==> stack(int32ToData(0))
      stackOfExec(prog.withStack(int32ToData(123), int32ToData(2), int32ToData(3)).opcode(MOD)) ==> stack(int32ToData(123), int32ToData(1))
      stackOfExec(prog.withStack(int32ToData(2), int32ToData(0)).opcode(MOD)) ==> stack(int32ToData(0))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(16)).opcode(MOD)) ==> stack(int32ToData(16))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(-16)).opcode(MOD)) ==> stack(int32ToData(-16))
      stackOfExec(prog.withStack(int32ToData(-17), int32ToData(16)).opcode(MOD)) ==> stack(int32ToData(16))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(33)).opcode(MOD)) ==> stack(int32ToData(16))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(50)).opcode(MOD)) ==> stack(int32ToData(16))

    }

    'fadd - {
      'sum - {
        val program = prog.withStack(doubleToData(1.0), doubleToData(2.0)).opcode(FADD)
        stackOfExec(program) ==> stack(doubleToData(3.0))

        // More bytes in stack
        val program2 = prog.withStack(doubleToData(77.0), doubleToData(880.0), doubleToData(13.0), doubleToData(24.0))
        stackOfExec(program2.opcode(FADD)) ==> stack(doubleToData(77.0), doubleToData(880.0), doubleToData(37.0))
      }

      'bigNumbers  - {

        stackOfExec(prog.withStack(doubleToData(257.0), doubleToData(258.0)).opcode(FADD)).last ==> doubleToData(515.0)
        stackOfExec(prog.withStack(doubleToData(32534.0), doubleToData(32535.0)).opcode(FADD)).last ==> doubleToData(65069.0)
        stackOfExec(prog.withStack(doubleToData(65535.0), doubleToData(65534.0)).opcode(FADD)).last ==> doubleToData(131069.0)
        stackOfExec(prog.withStack(doubleToData(1073741823.0), doubleToData(1073741822.0)).opcode(FADD)).last ==> doubleToData(2147483645.0)

      }

      'negativeNumbers - {

        stackOfExec(prog.withStack(doubleToData(2.0), doubleToData(-3.0)).opcode(FADD)) ==> stack(doubleToData(-1.0))
        stackOfExec(prog.withStack(doubleToData(-2.0), doubleToData(-3.0)).opcode(FADD)) ==> stack(doubleToData(-5.0))
        stackOfExec(prog.withStack(doubleToData(-2.0), doubleToData(3.0)).opcode(FADD)) ==> stack(doubleToData(1.0))

      }

      'zero - {

        stackOfExec(prog.withStack(doubleToData(-2.0), doubleToData(0.0)).opcode(FADD)) ==> stack(doubleToData(-2.0))
        stackOfExec(prog.withStack(doubleToData(0.0), doubleToData(0.0)).opcode(FADD)) ==> stack(doubleToData(0.0))

      }
    }

    'fmul - {
      stackOfExec(prog.withStack(doubleToData(-3.0), doubleToData(3.0)).opcode(FMUL)) ==> stack(doubleToData(-9.0))
      stackOfExec(prog.withStack(doubleToData(123.0), doubleToData(-3.0), doubleToData(3.0)).opcode(FMUL)) ==> stack(doubleToData(123.0), doubleToData(-9.0))
      stackOfExec(prog.withStack(doubleToData(0.0), doubleToData(3.0)).opcode(FMUL)) ==> stack(doubleToData(0.0))
      stackOfExec(prog.withStack(doubleToData(-3.0), doubleToData(0.0)).opcode(FMUL)) ==> stack(doubleToData(-0.0))
    }

    'fdiv - {
      stackOfExec(prog.withStack(doubleToData(-3.0), doubleToData(3.0)).opcode(FDIV)) ==> stack(doubleToData(-1.0))
      stackOfExec(prog.withStack(doubleToData(123.0), doubleToData(-3.0), doubleToData(3.0)).opcode(FDIV)) ==> stack(doubleToData(123.0), doubleToData(-1.0))
      stackOfExec(prog.withStack(doubleToData(2.0), doubleToData(0.0)).opcode(FDIV)) ==> stack(doubleToData(0.0))
      stackOfExec(prog.withStack(doubleToData(2.0), doubleToData(-5.0)).opcode(FDIV)) ==> stack(doubleToData(-2.5))
      stackOfExec(prog.withStack(doubleToData(2.0), doubleToData(5.0)).opcode(FDIV)) ==> stack(doubleToData(2.5))
    }
    
    'fmod - {
      stackOfExec(prog.withStack(int32ToData(-3), int32ToData(3)).opcode(MOD)) ==> stack(int32ToData(0))
      stackOfExec(prog.withStack(int32ToData(123), int32ToData(2), int32ToData(3)).opcode(MOD)) ==> stack(int32ToData(123), int32ToData(1))
      stackOfExec(prog.withStack(int32ToData(2), int32ToData(0)).opcode(MOD)) ==> stack(int32ToData(0))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(16)).opcode(MOD)) ==> stack(int32ToData(16))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(-16)).opcode(MOD)) ==> stack(int32ToData(-16))
      stackOfExec(prog.withStack(int32ToData(-17), int32ToData(16)).opcode(MOD)) ==> stack(int32ToData(16))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(33)).opcode(MOD)) ==> stack(int32ToData(16))
      stackOfExec(prog.withStack(int32ToData(17), int32ToData(50)).opcode(MOD)) ==> stack(int32ToData(16))
    }
  }
}
