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

trait Stack[+A] {
  def pop(n: Int): (List[A], Stack[A])
  def pop(): (A, Stack[A])
  def push[B >: A](a: B): Stack[B]
  def swap(a: Int): Stack[A]
  def dup(a: Int): Stack[A]
  def size: Int
}

case class StackList[+A](state: List[A]) extends Stack[A] {
  val size = state.size
  override def pop(n: Int): (List[A], Stack[A]) = state.take(n) -> new StackList(state.drop(n))
  override def pop(): (A, Stack[A]) = state.head -> new StackList(state.tail)
  override def push[B >: A](a: B): Stack[B] = new StackList(a :: state)
  override def swap(n: Int): Stack[A] =
    new StackList(state(n) :: state.tail.take(n - 1) ::: state.head :: state.drop(n + 1))
  override def dup(n: Int): Stack[A] = new StackList(state(n - 1) :: state)

  override def toString: String = state.toString.replace("List", "Stack")
}

object StackList {

  def empty[A]: Stack[A] = new StackList(List.empty[A])
  def apply[A](a: A*): Stack[A] = new StackList(a.toList)
}
