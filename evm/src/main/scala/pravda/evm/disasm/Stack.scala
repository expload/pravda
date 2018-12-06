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

import scala.annotation.tailrec

trait Stack[A] { self =>

  def pop(n: Int): (List[A], Stack[A]) = {
    @tailrec def aux(n: Int, acc: (List[A], Stack[A])): (List[A], Stack[A]) = n match {
      case 0 => acc._1.reverse -> acc._2
      case _ =>
        val (el, tail) = acc._2.pop()
        aux(n - 1, (el :: acc._1, tail))
    }
    aux(n, (Nil, self))
  }
  def pop(): (A, Stack[A])
  def push[B >: A](a: B): Stack[B]
  def swap(a: Int): Stack[A]
  def dup(a: Int): Stack[A]
  def size: Int
}

case class StackList[A](state: List[A]) extends Stack[A] {
  lazy val size: Int = state.size

  def pop(): (A, Stack[A]) = state.head -> new StackList(state.tail)
  def push[B >: A](a: B): Stack[B] = new StackList(a :: state)

  def swap(n: Int): Stack[A] =
    new StackList(state(n) :: state.tail.take(n - 1) ::: state.head :: state.drop(n + 1))
  def dup(n: Int): Stack[A] = new StackList(state(n - 1) :: state)
}

object StackList {
  def empty[A]: Stack[A] = new StackList(List.empty[A])
  def apply[A](a: A*): Stack[A] = new StackList(a.toList)
}
