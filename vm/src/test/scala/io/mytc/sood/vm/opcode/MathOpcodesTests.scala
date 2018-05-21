package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._
import utest._

object MathOpcodesTests extends TestSuite {

  val tests = Tests {
    'i32Add - {

      'sum - {

        val program = prog.withStack(data(2), data(3)).opcode(I32ADD)
        exec(program) ==> stack(data(5))

        // More bytes in stack
        val program2 = prog.withStack(data(77), data(880), data(13), data(24))
        exec(program2.opcode(I32ADD)) ==> stack(data(77), data(880), data(37))

      }

      'bigNumbers - {

        exec(prog.withStack(data(257), data(258)).opcode(I32ADD)).last ==> data(515)
        exec(prog.withStack(data(32534), data(32535)).opcode(I32ADD)).last ==> data(65069)
        exec(prog.withStack(data(65535), data(65534)).opcode(I32ADD)).last ==> data(131069)
        exec(prog.withStack(data(1073741823), data(1073741822)).opcode(I32ADD)).last ==> data(2147483645)

      }

      'negativeNumbers - {

        exec(prog.withStack(data(2), data(-3)).opcode(I32ADD)) ==> stack(data(-1))
        exec(prog.withStack(data(-2), data(-3)).opcode(I32ADD)) ==> stack(data(-5))
        exec(prog.withStack(data(-2), data(3)).opcode(I32ADD)) ==> stack(data(1))

      }

      'zero - {

        exec(prog.withStack(data(-2), data(0)).opcode(I32ADD)) ==> stack(data(-2))
        exec(prog.withStack(data(0), data(0)).opcode(I32ADD)) ==> stack(data(0))

      }
    }

    'i32mul - {
      exec(prog.withStack(data(-3), data(3)).opcode(I32MUL)) ==> stack(data(-9))
      exec(prog.withStack(data(123), data(-3), data(3)).opcode(I32MUL)) ==> stack(data(123), data(-9))
      exec(prog.withStack(data(0), data(3)).opcode(I32MUL)) ==> stack(data(0))
      exec(prog.withStack(data(-3), data(0)).opcode(I32MUL)) ==> stack(data(0))
    }

    'i32div - {
      exec(prog.withStack(data(-3), data(3)).opcode(I32DIV)) ==> stack(data(-1))
      exec(prog.withStack(data(123), data(-3), data(3)).opcode(I32DIV)) ==> stack(data(123), data(-1))
      exec(prog.withStack(data(2), data(0)).opcode(I32DIV)) ==> stack(data(0))
      exec(prog.withStack(data(2), data(-5)).opcode(I32DIV)) ==> stack(data(-2))
      exec(prog.withStack(data(2), data(5)).opcode(I32DIV)) ==> stack(data(2))
    }

    'i32mod - {

      exec(prog.withStack(data(-3), data(3)).opcode(I32MOD)) ==> stack(data(0))
      exec(prog.withStack(data(123), data(2), data(3)).opcode(I32MOD)) ==> stack(data(123), data(1))
      exec(prog.withStack(data(2), data(0)).opcode(I32MOD)) ==> stack(data(0))
      exec(prog.withStack(data(17), data(16)).opcode(I32MOD)) ==> stack(data(16))
      exec(prog.withStack(data(17), data(-16)).opcode(I32MOD)) ==> stack(data(-16))
      exec(prog.withStack(data(-17), data(16)).opcode(I32MOD)) ==> stack(data(16))
      exec(prog.withStack(data(17), data(33)).opcode(I32MOD)) ==> stack(data(16))
      exec(prog.withStack(data(17), data(50)).opcode(I32MOD)) ==> stack(data(16))

    }

    'fadd - {
      'sum - {
        val program = prog.withStack(data(1.0), data(2.0)).opcode(FADD)
        exec(program) ==> stack(data(3.0))

        // More bytes in stack
        val program2 = prog.withStack(data(77.0), data(880.0), data(13.0), data(24.0))
        exec(program2.opcode(FADD)) ==> stack(data(77.0), data(880.0), data(37.0))
      }

      'bigNumbers  - {

        exec(prog.withStack(data(257.0), data(258.0)).opcode(FADD)).last ==> data(515.0)
        exec(prog.withStack(data(32534.0), data(32535.0)).opcode(FADD)).last ==> data(65069.0)
        exec(prog.withStack(data(65535.0), data(65534.0)).opcode(FADD)).last ==> data(131069.0)
        exec(prog.withStack(data(1073741823.0), data(1073741822.0)).opcode(FADD)).last ==> data(2147483645.0)

      }

      'negativeNumbers - {

        exec(prog.withStack(data(2.0), data(-3.0)).opcode(FADD)) ==> stack(data(-1.0))
        exec(prog.withStack(data(-2.0), data(-3.0)).opcode(FADD)) ==> stack(data(-5.0))
        exec(prog.withStack(data(-2.0), data(3.0)).opcode(FADD)) ==> stack(data(1.0))

      }

      'zero - {

        exec(prog.withStack(data(-2.0), data(0.0)).opcode(FADD)) ==> stack(data(-2.0))
        exec(prog.withStack(data(0.0), data(0.0)).opcode(FADD)) ==> stack(data(0.0))

      }
    }

    'fmul - {
      exec(prog.withStack(data(-3.0), data(3.0)).opcode(FMUL)) ==> stack(data(-9.0))
      exec(prog.withStack(data(123.0), data(-3.0), data(3.0)).opcode(FMUL)) ==> stack(data(123.0), data(-9.0))
      exec(prog.withStack(data(0.0), data(3.0)).opcode(FMUL)) ==> stack(data(0.0))
      exec(prog.withStack(data(-3.0), data(0.0)).opcode(FMUL)) ==> stack(data(-0.0))
    }

    'fdiv - {
      exec(prog.withStack(data(-3.0), data(3.0)).opcode(FDIV)) ==> stack(data(-1.0))
      exec(prog.withStack(data(123.0), data(-3.0), data(3.0)).opcode(FDIV)) ==> stack(data(123.0), data(-1.0))
      exec(prog.withStack(data(2.0), data(0.0)).opcode(FDIV)) ==> stack(data(0.0))
      exec(prog.withStack(data(2.0), data(-5.0)).opcode(FDIV)) ==> stack(data(-2.5))
      exec(prog.withStack(data(2.0), data(5.0)).opcode(FDIV)) ==> stack(data(2.5))
    }
    
    'fmod - {
      exec(prog.withStack(data(-3), data(3)).opcode(I32MOD)) ==> stack(data(0))
      exec(prog.withStack(data(123), data(2), data(3)).opcode(I32MOD)) ==> stack(data(123), data(1))
      exec(prog.withStack(data(2), data(0)).opcode(I32MOD)) ==> stack(data(0))
      exec(prog.withStack(data(17), data(16)).opcode(I32MOD)) ==> stack(data(16))
      exec(prog.withStack(data(17), data(-16)).opcode(I32MOD)) ==> stack(data(-16))
      exec(prog.withStack(data(-17), data(16)).opcode(I32MOD)) ==> stack(data(16))
      exec(prog.withStack(data(17), data(33)).opcode(I32MOD)) ==> stack(data(16))
      exec(prog.withStack(data(17), data(50)).opcode(I32MOD)) ==> stack(data(16))
    }
  }
}
