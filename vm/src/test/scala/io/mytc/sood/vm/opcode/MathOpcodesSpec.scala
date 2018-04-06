package io.mytc.sood.vm
package opcode

import VmUtils._
import Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class MathOpcodesSpec extends FlatSpec with Matchers {

  "I32ADD opcode" should "sum up two top words from the stack" in {
    val program = prog.withStack(int32ToWord(2), int32ToWord(3)).opcode(I32ADD)
    exec(program) shouldBe stack(int32ToWord(5))

    // More bytes in stack
    val program2 = prog.withStack(int32ToWord(77), int32ToWord(880), int32ToWord(13), int32ToWord(24))
    exec(program2.opcode(I32ADD)) shouldBe stack(int32ToWord(77), int32ToWord(880), int32ToWord(37))

  }

  it should "work with big numbers" in {
    exec(prog.withStack(int32ToWord(257), int32ToWord(258)).opcode(I32ADD)).last shouldBe int32ToWord(515)
    exec(prog.withStack(int32ToWord(32534), int32ToWord(32535)).opcode(I32ADD)).last shouldBe int32ToWord(65069)
    exec(prog.withStack(int32ToWord(65535), int32ToWord(65534)).opcode(I32ADD)).last shouldBe int32ToWord(131069)
    exec(prog.withStack(int32ToWord(1073741823), int32ToWord(1073741822)).opcode(I32ADD)).last shouldBe int32ToWord(2147483645)
  }

  it should "work with negative numbers" in {
    exec(prog.withStack(int32ToWord(2), int32ToWord(-3)).opcode(I32ADD)) shouldBe stack(int32ToWord(-1))
    exec(prog.withStack(int32ToWord(-2), int32ToWord(-3)).opcode(I32ADD)) shouldBe stack(int32ToWord(-5))
    exec(prog.withStack(int32ToWord(-2), int32ToWord(3)).opcode(I32ADD)) shouldBe stack(int32ToWord(1))
  }

  it should "work with zero" in {
    exec(prog.withStack(int32ToWord(-2), int32ToWord(0)).opcode(I32ADD)) shouldBe stack(int32ToWord(-2))
    exec(prog.withStack(int32ToWord(0), int32ToWord(0)).opcode(I32ADD)) shouldBe stack(int32ToWord(0))
  }

  "I32MUL opcode" should "multiply two top words together" in {
    exec(prog.withStack(int32ToWord(-3), int32ToWord(3)).opcode(I32MUL)) shouldBe stack(int32ToWord(-9))
    exec(prog.withStack(int32ToWord(123), int32ToWord(-3), int32ToWord(3)).opcode(I32MUL)) shouldBe stack(int32ToWord(123), int32ToWord(-9))
    exec(prog.withStack(int32ToWord(0), int32ToWord(3)).opcode(I32MUL)) shouldBe stack(int32ToWord(0))
    exec(prog.withStack(int32ToWord(-3), int32ToWord(0)).opcode(I32MUL)) shouldBe stack(int32ToWord(0))
  }

  "I32DIV opcode" should "devide first word of the stack by the second word of the stack" in {
    exec(prog.withStack(int32ToWord(-3), int32ToWord(3)).opcode(I32DIV)) shouldBe stack(int32ToWord(-1))
    exec(prog.withStack(int32ToWord(123), int32ToWord(-3), int32ToWord(3)).opcode(I32DIV)) shouldBe stack(int32ToWord(123), int32ToWord(-1))
    exec(prog.withStack(int32ToWord(2), int32ToWord(0)).opcode(I32DIV)) shouldBe stack(int32ToWord(0))
    exec(prog.withStack(int32ToWord(2), int32ToWord(-5)).opcode(I32DIV)) shouldBe stack(int32ToWord(-2))
    exec(prog.withStack(int32ToWord(2), int32ToWord(5)).opcode(I32DIV)) shouldBe stack(int32ToWord(2))
  }

  "I32MOD opcode" should "returns the remainder of division of the 1st word by the 2nd word" in {
    exec(prog.withStack(int32ToWord(-3), int32ToWord(3)).opcode(I32MOD)) shouldBe stack(int32ToWord(0))
    exec(prog.withStack(int32ToWord(123), int32ToWord(2), int32ToWord(3)).opcode(I32MOD)) shouldBe stack(int32ToWord(123), int32ToWord(1))
    exec(prog.withStack(int32ToWord(2), int32ToWord(0)).opcode(I32MOD)) shouldBe stack(int32ToWord(0))
    exec(prog.withStack(int32ToWord(17), int32ToWord(16)).opcode(I32MOD)) shouldBe stack(int32ToWord(16))
    exec(prog.withStack(int32ToWord(17), int32ToWord(-16)).opcode(I32MOD)) shouldBe stack(int32ToWord(-16))
    exec(prog.withStack(int32ToWord(-17), int32ToWord(16)).opcode(I32MOD)) shouldBe stack(int32ToWord(16))
    exec(prog.withStack(int32ToWord(17), int32ToWord(33)).opcode(I32MOD)) shouldBe stack(int32ToWord(16))
    exec(prog.withStack(int32ToWord(17), int32ToWord(50)).opcode(I32MOD)) shouldBe stack(int32ToWord(16))
  }

}
