package io.mytc.sood.vm
package opcode

import VmUtils._
import Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class MathOpcodesSpec extends FlatSpec with Matchers {

  "I32ADD opcode" should "sum up two top words from the stack" in {
    val program = prog.withStack(data(2), data(3)).opcode(I32ADD)
    exec(program) shouldBe stack(data(5))

    // More bytes in stack
    val program2 = prog.withStack(data(77), data(880), data(13), data(24))
    exec(program2.opcode(I32ADD)) shouldBe stack(data(77), data(880), data(37))

  }

  it should "work with big numbers" in {
    exec(prog.withStack(data(257), data(258)).opcode(I32ADD)).last shouldBe data(515)
    exec(prog.withStack(data(32534), data(32535)).opcode(I32ADD)).last shouldBe data(65069)
    exec(prog.withStack(data(65535), data(65534)).opcode(I32ADD)).last shouldBe data(131069)
    exec(prog.withStack(data(1073741823), data(1073741822)).opcode(I32ADD)).last shouldBe data(2147483645)
  }

  it should "work with negative numbers" in {
    exec(prog.withStack(data(2), data(-3)).opcode(I32ADD)) shouldBe stack(data(-1))
    exec(prog.withStack(data(-2), data(-3)).opcode(I32ADD)) shouldBe stack(data(-5))
    exec(prog.withStack(data(-2), data(3)).opcode(I32ADD)) shouldBe stack(data(1))
  }

  it should "work with zero" in {
    exec(prog.withStack(data(-2), data(0)).opcode(I32ADD)) shouldBe stack(data(-2))
    exec(prog.withStack(data(0), data(0)).opcode(I32ADD)) shouldBe stack(data(0))
  }

  "I32MUL opcode" should "multiply two top words together" in {
    exec(prog.withStack(data(-3), data(3)).opcode(I32MUL)) shouldBe stack(data(-9))
    exec(prog.withStack(data(123), data(-3), data(3)).opcode(I32MUL)) shouldBe stack(data(123), data(-9))
    exec(prog.withStack(data(0), data(3)).opcode(I32MUL)) shouldBe stack(data(0))
    exec(prog.withStack(data(-3), data(0)).opcode(I32MUL)) shouldBe stack(data(0))
  }

  "I32DIV opcode" should "devide first word of the stack by the second word of the stack" in {
    exec(prog.withStack(data(-3), data(3)).opcode(I32DIV)) shouldBe stack(data(-1))
    exec(prog.withStack(data(123), data(-3), data(3)).opcode(I32DIV)) shouldBe stack(data(123), data(-1))
    exec(prog.withStack(data(2), data(0)).opcode(I32DIV)) shouldBe stack(data(0))
    exec(prog.withStack(data(2), data(-5)).opcode(I32DIV)) shouldBe stack(data(-2))
    exec(prog.withStack(data(2), data(5)).opcode(I32DIV)) shouldBe stack(data(2))
  }

  "I32MOD opcode" should "returns the remainder of division of the 1st word by the 2nd word" in {
    exec(prog.withStack(data(-3), data(3)).opcode(I32MOD)) shouldBe stack(data(0))
    exec(prog.withStack(data(123), data(2), data(3)).opcode(I32MOD)) shouldBe stack(data(123), data(1))
    exec(prog.withStack(data(2), data(0)).opcode(I32MOD)) shouldBe stack(data(0))
    exec(prog.withStack(data(17), data(16)).opcode(I32MOD)) shouldBe stack(data(16))
    exec(prog.withStack(data(17), data(-16)).opcode(I32MOD)) shouldBe stack(data(-16))
    exec(prog.withStack(data(-17), data(16)).opcode(I32MOD)) shouldBe stack(data(16))
    exec(prog.withStack(data(17), data(33)).opcode(I32MOD)) shouldBe stack(data(16))
    exec(prog.withStack(data(17), data(50)).opcode(I32MOD)) shouldBe stack(data(16))
  }

}
