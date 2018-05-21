package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._
import utest._

object MathOpcodesTests extends TestSuite {

  val tests = Tests {
    'i32Add - {

      'sum - {

        val program = prog.withStack(int2Data(2), int2Data(3)).opcode(I32ADD)
        exec(program) ==> stack(int2Data(5))

        // More bytes in stack
        val program2 = prog.withStack(int2Data(77), int2Data(880), int2Data(13), int2Data(24))
        exec(program2.opcode(I32ADD)) ==> stack(int2Data(77), int2Data(880), int2Data(37))

      }

      'bigNumbers - {

        exec(prog.withStack(int2Data(257), int2Data(258)).opcode(I32ADD)).last ==> int2Data(515)
        exec(prog.withStack(int2Data(32534), int2Data(32535)).opcode(I32ADD)).last ==> int2Data(65069)
        exec(prog.withStack(int2Data(65535), int2Data(65534)).opcode(I32ADD)).last ==> int2Data(131069)
        exec(prog.withStack(int2Data(1073741823), int2Data(1073741822)).opcode(I32ADD)).last ==> int2Data(2147483645)

      }

      'negativeNumbers - {

        exec(prog.withStack(int2Data(2), int2Data(-3)).opcode(I32ADD)) ==> stack(int2Data(-1))
        exec(prog.withStack(int2Data(-2), int2Data(-3)).opcode(I32ADD)) ==> stack(int2Data(-5))
        exec(prog.withStack(int2Data(-2), int2Data(3)).opcode(I32ADD)) ==> stack(int2Data(1))

      }

      'zero - {

        exec(prog.withStack(int2Data(-2), int2Data(0)).opcode(I32ADD)) ==> stack(int2Data(-2))
        exec(prog.withStack(int2Data(0), int2Data(0)).opcode(I32ADD)) ==> stack(int2Data(0))

      }
    }

    'i32mul - {
      exec(prog.withStack(int2Data(-3), int2Data(3)).opcode(I32MUL)) ==> stack(int2Data(-9))
      exec(prog.withStack(int2Data(123), int2Data(-3), int2Data(3)).opcode(I32MUL)) ==> stack(int2Data(123), int2Data(-9))
      exec(prog.withStack(int2Data(0), int2Data(3)).opcode(I32MUL)) ==> stack(int2Data(0))
      exec(prog.withStack(int2Data(-3), int2Data(0)).opcode(I32MUL)) ==> stack(int2Data(0))
    }

    'i32div - {
      exec(prog.withStack(int2Data(-3), int2Data(3)).opcode(I32DIV)) ==> stack(int2Data(-1))
      exec(prog.withStack(int2Data(123), int2Data(-3), int2Data(3)).opcode(I32DIV)) ==> stack(int2Data(123), int2Data(-1))
      exec(prog.withStack(int2Data(2), int2Data(0)).opcode(I32DIV)) ==> stack(int2Data(0))
      exec(prog.withStack(int2Data(2), int2Data(-5)).opcode(I32DIV)) ==> stack(int2Data(-2))
      exec(prog.withStack(int2Data(2), int2Data(5)).opcode(I32DIV)) ==> stack(int2Data(2))
    }

    'i32mod - {

      exec(prog.withStack(int2Data(-3), int2Data(3)).opcode(I32MOD)) ==> stack(int2Data(0))
      exec(prog.withStack(int2Data(123), int2Data(2), int2Data(3)).opcode(I32MOD)) ==> stack(int2Data(123), int2Data(1))
      exec(prog.withStack(int2Data(2), int2Data(0)).opcode(I32MOD)) ==> stack(int2Data(0))
      exec(prog.withStack(int2Data(17), int2Data(16)).opcode(I32MOD)) ==> stack(int2Data(16))
      exec(prog.withStack(int2Data(17), int2Data(-16)).opcode(I32MOD)) ==> stack(int2Data(-16))
      exec(prog.withStack(int2Data(-17), int2Data(16)).opcode(I32MOD)) ==> stack(int2Data(16))
      exec(prog.withStack(int2Data(17), int2Data(33)).opcode(I32MOD)) ==> stack(int2Data(16))
      exec(prog.withStack(int2Data(17), int2Data(50)).opcode(I32MOD)) ==> stack(int2Data(16))

    }

    'fadd - {
      'sum - {
        val program = prog.withStack(float2Data(1.0), float2Data(2.0)).opcode(FADD)
        exec(program) ==> stack(float2Data(3.0))

        // More bytes in stack
        val program2 = prog.withStack(float2Data(77.0), float2Data(880.0), float2Data(13.0), float2Data(24.0))
        exec(program2.opcode(FADD)) ==> stack(float2Data(77.0), float2Data(880.0), float2Data(37.0))
      }

      'bigNumbers  - {

        exec(prog.withStack(float2Data(257.0), float2Data(258.0)).opcode(FADD)).last ==> float2Data(515.0)
        exec(prog.withStack(float2Data(32534.0), float2Data(32535.0)).opcode(FADD)).last ==> float2Data(65069.0)
        exec(prog.withStack(float2Data(65535.0), float2Data(65534.0)).opcode(FADD)).last ==> float2Data(131069.0)
        exec(prog.withStack(float2Data(1073741823.0), float2Data(1073741822.0)).opcode(FADD)).last ==> float2Data(2147483645.0)

      }

      'negativeNumbers - {

        exec(prog.withStack(float2Data(2.0), float2Data(-3.0)).opcode(FADD)) ==> stack(float2Data(-1.0))
        exec(prog.withStack(float2Data(-2.0), float2Data(-3.0)).opcode(FADD)) ==> stack(float2Data(-5.0))
        exec(prog.withStack(float2Data(-2.0), float2Data(3.0)).opcode(FADD)) ==> stack(float2Data(1.0))

      }

      'zero - {

        exec(prog.withStack(float2Data(-2.0), float2Data(0.0)).opcode(FADD)) ==> stack(float2Data(-2.0))
        exec(prog.withStack(float2Data(0.0), float2Data(0.0)).opcode(FADD)) ==> stack(float2Data(0.0))

      }
    }

    'fmul - {
      exec(prog.withStack(float2Data(-3.0), float2Data(3.0)).opcode(FMUL)) ==> stack(float2Data(-9.0))
      exec(prog.withStack(float2Data(123.0), float2Data(-3.0), float2Data(3.0)).opcode(FMUL)) ==> stack(float2Data(123.0), float2Data(-9.0))
      exec(prog.withStack(float2Data(0.0), float2Data(3.0)).opcode(FMUL)) ==> stack(float2Data(0.0))
      exec(prog.withStack(float2Data(-3.0), float2Data(0.0)).opcode(FMUL)) ==> stack(float2Data(-0.0))
    }

    'fdiv - {
      exec(prog.withStack(float2Data(-3.0), float2Data(3.0)).opcode(FDIV)) ==> stack(float2Data(-1.0))
      exec(prog.withStack(float2Data(123.0), float2Data(-3.0), float2Data(3.0)).opcode(FDIV)) ==> stack(float2Data(123.0), float2Data(-1.0))
      exec(prog.withStack(float2Data(2.0), float2Data(0.0)).opcode(FDIV)) ==> stack(float2Data(0.0))
      exec(prog.withStack(float2Data(2.0), float2Data(-5.0)).opcode(FDIV)) ==> stack(float2Data(-2.5))
      exec(prog.withStack(float2Data(2.0), float2Data(5.0)).opcode(FDIV)) ==> stack(float2Data(2.5))
    }
    
    'fmod - {
      exec(prog.withStack(int2Data(-3), int2Data(3)).opcode(I32MOD)) ==> stack(int2Data(0))
      exec(prog.withStack(int2Data(123), int2Data(2), int2Data(3)).opcode(I32MOD)) ==> stack(int2Data(123), int2Data(1))
      exec(prog.withStack(int2Data(2), int2Data(0)).opcode(I32MOD)) ==> stack(int2Data(0))
      exec(prog.withStack(int2Data(17), int2Data(16)).opcode(I32MOD)) ==> stack(int2Data(16))
      exec(prog.withStack(int2Data(17), int2Data(-16)).opcode(I32MOD)) ==> stack(int2Data(-16))
      exec(prog.withStack(int2Data(-17), int2Data(16)).opcode(I32MOD)) ==> stack(int2Data(16))
      exec(prog.withStack(int2Data(17), int2Data(33)).opcode(I32MOD)) ==> stack(int2Data(16))
      exec(prog.withStack(int2Data(17), int2Data(50)).opcode(I32MOD)) ==> stack(int2Data(16))
    }
  }
}
