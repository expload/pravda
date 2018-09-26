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

package pravda.vm

trait Memory {
  def stack: Seq[Data.Primitive]
  def heap: Seq[Data]
  def limit(index: Int): Unit
  def dropLimit(): Unit
  def pop(): Data.Primitive
  def push(x: Data.Primitive): Unit
  def get(i: Int): Data.Primitive
  def clear(): Unit
  def all: Seq[Data.Primitive]
  def swap(i: Int, j: Int): Unit
  def length: Int
  // TODO should return Data.Ref
  def heapPut(x: Data): Int
  // TODO should take Data.Ref
  def heapGet(idx: Int): Data
  def heapLength: Int
  def top(): Data.Primitive
  def top(n: Int): Seq[Data.Primitive]
}
