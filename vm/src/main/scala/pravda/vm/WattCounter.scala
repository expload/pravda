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

import pravda.common.vm.Data

trait WattCounter {

  def refund: Long

  def spent: Long

  def total: Long

  def limit: Long

  def storageUsage(occupiedBytes: Long = 0L, releasedBytes: Long = 0L)

  def memoryUsage(memoryBytesInUse: Long): Unit

  def cpuUsage(timeUnits: Long*): Unit

  def extra(watts: Long): Unit

  def refund(watts: Long): Unit

  def check(): Unit
}

object WattCounter {

  val StorageOccupyFactor = 8L
  val StorageReleaseFactor = 2L
  val CpuFactor = 1L

  val CpuBasic = 1L
  val CpuSimpleArithmetic = 5L
  val CpuArithmetic = 10L

  val CpuProgControl = 5L
  val CpuExtCall = 10L
  val CpuStorageUse = 20L

  private val CpuWordDelimiter = 64L

  def CpuWordOperation(word: Data*): Long =
    word.map(_.volume).sum / CpuWordDelimiter
}
