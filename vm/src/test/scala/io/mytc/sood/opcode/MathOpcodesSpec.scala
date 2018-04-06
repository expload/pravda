package io.mytc.sood
package opcode

import io.mytc.sood.vm.Opcodes.int._
import serialize._
import org.scalatest.{FlatSpec, Matchers}

class MathOpcodesSpec extends FlatSpec with Matchers {

  "I32ADD opcode" should "sum up two top words from the stack" in {
    val program = prog.withStack(pureWord(2), pureWord(3)).opcode(I32ADD)
    exec(program) shouldBe stack(pureWord(5))

    // More bytes in stack
    val program2 = prog.withStack(pureWord(77), pureWord(880), pureWord(13), pureWord(24))
    exec(program2.opcode(I32ADD)) shouldBe stack(pureWord(77), pureWord(880), pureWord(37))

  }

  it should "work with big numbers" in {
    exec(prog.withStack(pureWord(257), pureWord(258)).opcode(I32ADD)).last shouldBe pureWord(515)
    exec(prog.withStack(pureWord(32534), pureWord(32535)).opcode(I32ADD)).last shouldBe pureWord(65069)
    exec(prog.withStack(pureWord(65535), pureWord(65534)).opcode(I32ADD)).last shouldBe pureWord(131069)
    exec(prog.withStack(pureWord(1073741823), pureWord(1073741822)).opcode(I32ADD)).last shouldBe pureWord(2147483645)
  }

  it should "work with negative numbers" in {
    exec(prog.withStack(pureWord(2), pureWord(-3)).opcode(I32ADD)) shouldBe stack(pureWord(-1))
    exec(prog.withStack(pureWord(-2), pureWord(-3)).opcode(I32ADD)) shouldBe stack(pureWord(-5))
    exec(prog.withStack(pureWord(-2), pureWord(3)).opcode(I32ADD)) shouldBe stack(pureWord(1))
  }

  it should "work with zero" in {
    exec(prog.withStack(pureWord(-2), pureWord(0)).opcode(I32ADD)) shouldBe stack(pureWord(-2))
    exec(prog.withStack(pureWord(0), pureWord(0)).opcode(I32ADD)) shouldBe stack(pureWord(0))
  }

  "I32MUL opcode" should "multiply two top words together" in {
    exec(prog.withStack(pureWord(-3), pureWord(3)).opcode(I32MUL)) shouldBe stack(pureWord(-9))
    exec(prog.withStack(pureWord(123), pureWord(-3), pureWord(3)).opcode(I32MUL)) shouldBe stack(pureWord(123), pureWord(-9))
    exec(prog.withStack(pureWord(0), pureWord(3)).opcode(I32MUL)) shouldBe stack(pureWord(0))
    exec(prog.withStack(pureWord(-3), pureWord(0)).opcode(I32MUL)) shouldBe stack(pureWord(0))
  }

  "I32DIV opcode" should "devide first word of the stack by the second word of the stack" in {
    exec(prog.withStack(pureWord(-3), pureWord(3)).opcode(I32DIV)) shouldBe stack(pureWord(-1))
    exec(prog.withStack(pureWord(123), pureWord(-3), pureWord(3)).opcode(I32DIV)) shouldBe stack(pureWord(123), pureWord(-1))
    exec(prog.withStack(pureWord(2), pureWord(0)).opcode(I32DIV)) shouldBe stack(pureWord(0))
    exec(prog.withStack(pureWord(2), pureWord(-5)).opcode(I32DIV)) shouldBe stack(pureWord(-2))
    exec(prog.withStack(pureWord(2), pureWord(5)).opcode(I32DIV)) shouldBe stack(pureWord(2))
  }

  "I32MOD opcode" should "returns the remainder of division of the 1st word by the 2nd word" in {
    exec(prog.withStack(pureWord(-3), pureWord(3)).opcode(I32MOD)) shouldBe stack(pureWord(0))
    exec(prog.withStack(pureWord(123), pureWord(2), pureWord(3)).opcode(I32MOD)) shouldBe stack(pureWord(123), pureWord(1))
    exec(prog.withStack(pureWord(2), pureWord(0)).opcode(I32MOD)) shouldBe stack(pureWord(0))
    exec(prog.withStack(pureWord(17), pureWord(16)).opcode(I32MOD)) shouldBe stack(pureWord(16))
    exec(prog.withStack(pureWord(17), pureWord(-16)).opcode(I32MOD)) shouldBe stack(pureWord(-16))
    exec(prog.withStack(pureWord(-17), pureWord(16)).opcode(I32MOD)) shouldBe stack(pureWord(16))
    exec(prog.withStack(pureWord(17), pureWord(33)).opcode(I32MOD)) shouldBe stack(pureWord(16))
    exec(prog.withStack(pureWord(17), pureWord(50)).opcode(I32MOD)) shouldBe stack(pureWord(16))
  }

}
