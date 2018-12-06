/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.evm.disasm

import pravda.evm.EVM.{Op, _}
import pravda.evm.disasm.Blocks.{WithJumpI, WithJumpDest}

import scala.annotation.tailrec

trait StackItem

trait Expr extends StackItem

case class Expr0(op: Op)                                                                extends Expr
case class Expr1(op: Op, operand1: StackItem)                                           extends Expr
case class Expr2(op: Op, operand1: StackItem, operand2: StackItem)                      extends Expr
case class Expr3(op: Op, operand1: StackItem, operand2: StackItem, operand3: StackItem) extends Expr

case class Number(n: BigInt)                extends StackItem
case class CommandResult(op: Op, numb: Int) extends StackItem

case class HistoryRecord(op: Op, args: List[StackItem])

object Emulator {

  @tailrec def eval(ops: List[Op],
                    state: Stack[StackItem],
                    history: List[HistoryRecord]): (Stack[StackItem], List[HistoryRecord]) = {
    ops match {
      case Nil                             => state -> history
      case Push(n) :: xs                   => eval(xs, state push Number(BigInt(1, n.toArray)), HistoryRecord(Push(n), Nil) :: history)
      case Swap(n) :: xs if n < state.size => eval(xs, state swap n, history)
      case Dup(n) :: xs if n <= state.size => eval(xs, state dup n, history)

      case op :: ops =>
        val r = OpCodes.stackReadCount(op)
        val (args, s) = if (state.size >= r) state.pop(r) else state.pop(state.size)
        val w = OpCodes.stackWriteCount(op)
        val resState = (1 to w).foldLeft(s) { case (state, n) => state push CommandResult(op, n) }
        eval(ops, resState, HistoryRecord(op, args) :: history)
    }
  }

  def eval(main: List[List[Op]],
           withJ: Map[Int, WithJumpDest],
           WithJumpI: Map[Int, WithJumpI]): (Set[AddressedJumpOp], Set[WithJumpDest]) = {

    def evalBlock(state: Stack[StackItem])(history: List[HistoryRecord])(
        block: List[Op]): (Option[AddressedJumpOp], Stack[StackItem], List[HistoryRecord]) = {
      val (newStack, newHistory) = Emulator.eval(block, state, history)
      jump(newHistory) match {
        case Some(HistoryRecord(SelfAddressedJumpI(n), Number(dest) :: _)) =>
          (Some(JumpI(n, dest.intValue())), newStack, Nil)
        case Some(HistoryRecord(SelfAddressedJump(n), Number(dest) :: _)) =>
          (Some(Jump(n, dest.intValue())), newStack, Nil)

        case Some(r @ HistoryRecord(SelfAddressedJump(n), _)) =>
          (None, newStack, newHistory)
        case Some(r @ HistoryRecord(SelfAddressedJumpI(n), _)) =>
          (None, newStack, newHistory)

        case _ => (None, newStack, newHistory)
      }
    }

    def evalChain(state: Stack[StackItem])(history: List[HistoryRecord])(
        ops: List[Op],
        withJ: Map[Int, WithJumpDest],
        WithJumpI: Map[Int, WithJumpI],
        acc: Set[AddressedJumpOp]): (Set[AddressedJumpOp], Map[Int, WithJumpDest], Map[Int, WithJumpI]) = {
      val (jump, newStack, newHistory) = evalBlock(state)(history)(ops)
      jump match {
        case Some(j @ Jump(addr, dest)) if withJ.contains(dest) =>
          evalChain(newStack)(newHistory)(withJ(dest).ops, withJ - dest, WithJumpI, acc + j)

        case Some(j @ JumpI(addr, dest)) if withJ.contains(dest) && !WithJumpI.contains(addr) =>
          evalChain(newStack)(newHistory)(withJ(dest).ops, withJ - dest, WithJumpI, acc + j)

        case Some(j @ JumpI(addr, dest)) if withJ.contains(dest) && WithJumpI.contains(addr) =>
          val (jumps1, dests1, contins1) =
            evalChain(newStack)(newHistory)(withJ(dest).ops, withJ - dest, WithJumpI - addr, acc + j)
          val (jumps2, dests2, contins2) =
            evalChain(newStack)(newHistory)(WithJumpI(addr).ops, withJ - dest, WithJumpI - addr, acc + j)
          val dests = dests1.keySet.intersect(dests2.keySet).map(k => k -> dests1(k)).toMap
          val contins = contins1.keySet.intersect(contins2.keySet).map(k => k -> contins1(k)).toMap
          (jumps1 ++ jumps2 ++ acc, dests, contins)

        case Some(j @ Jump(addr, dest)) => (acc + j, withJ, WithJumpI)

        case Some(j @ JumpI(addr, dest)) => (acc + j, withJ, WithJumpI)
        case _                           => (acc, withJ, WithJumpI)
      }
    }

    val (jumps1, jumpDests1, jumpi1) = main.foldLeft((Set.empty[AddressedJumpOp], withJ, WithJumpI)) {
      case ((jumps, dests, contins), ops) =>
        evalChain(StackList.empty)(List.empty)(ops, dests, contins, jumps)
    }
    val (jumps2, jumpDests2, jumpi2) = jumpi1.foldLeft((jumps1, jumpDests1, WithJumpI)) {
      case ((jumps, dests, contins), (_, ops)) =>
        evalChain(StackList.empty)(List.empty)(ops.ops, dests, contins, jumps)
    }
    val (jumps3, jumpDests3, jumpi3) = jumpDests2.foldLeft((jumps2, jumpDests2, jumpi2)) {
      case ((jumps, dests, contins), (_, ops)) =>
        evalChain(StackList.empty)(List.empty)(ops.ops, dests, contins, jumps)
    }
    jumps3 -> jumpDests3.values.toSet
  }

  def jump(jump: List[HistoryRecord]): Option[HistoryRecord] =
    jump.collectFirst {
      case h @ HistoryRecord(SelfAddressedJumpI(_), _) => h
      case h @ HistoryRecord(SelfAddressedJump(_), _)  => h
    }

  def jumps(blocks: List[List[Op]]): (Set[AddressedJumpOp], Set[WithJumpDest]) = {
    val jumpable = Blocks.jumpable(blocks)
    val main = jumpable.withoutJumpdest.filter {
      case SelfAddressedJumpI(_) :: _ => false
      case _                          => true
    }
    val byJumpdest = jumpable.withJumpdest.groupBy(_.dest).map { case (k, v)     => k.addr -> v.head }
    val byJumpi = Blocks.continuation(blocks).groupBy(_.jumpi).map { case (k, v) => k.addr -> v.head }
    Emulator.eval(main, byJumpdest, byJumpi)
  }
}
