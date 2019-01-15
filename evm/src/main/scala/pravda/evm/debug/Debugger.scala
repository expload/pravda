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

package pravda.evm.debug

import java.nio.ByteBuffer

import cats.Show
import pravda.vm.impl.MemoryImpl

import scala.util.Try

trait Debugger[L] {
  def debugOp(state: L, program: ByteBuffer, op: Int, mem: MemoryImpl)(exec: () => Try[Unit]): (L, Boolean)
  def initial: L
  def log(state: L)(implicit show: Show[L]): String = show.show(state)
}
