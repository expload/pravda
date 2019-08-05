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

package pravda.vm.impl

import pravda.common.vm.Error.OutOfWatts
import pravda.vm.{ThrowableVmError, WattCounter}

final class WattCounterImpl(val limit: Long) extends WattCounter {

  import WattCounter._

  private var cpuWatts = 0L
  private var storageWatts = 0L
  private var memoryWatts = 0L
  private var extraWatts = 0L
  private var storageRefund = 0L
  private var extraRefund = 0L

  def refund: Long =
    storageRefund + extraRefund

  def spent: Long =
    extraWatts + cpuWatts + memoryWatts + storageWatts

  def storageUsage(occupiedBytes: Long = 0L, releasedBytes: Long = 0L): Unit = {
    storageWatts += occupiedBytes * StorageOccupyFactor
    storageRefund += releasedBytes * StorageReleaseFactor
    check()
  }

  def memoryUsage(memoryBytesInUse: Long): Unit = {
    val M100 = 100000000L
    def cube(v: Long) = v * v * v
    memoryWatts = 100 * cube(memoryBytesInUse / M100 + 1) // TODO better function
    check()
  }

  def cpuUsage(timeUnits: Long*): Unit = {
    cpuWatts += timeUnits.sum * CpuFactor
    check()
  }

  def extra(watts: Long): Unit = {
    extraWatts += watts
    check()
  }

  def refund(watts: Long): Unit = {
    extraRefund += watts
  }

  def check(): Unit = {
    if (spent > limit) throw ThrowableVmError(OutOfWatts)
  }

  def total: Long = {
    val spentWatts = spent
    if (spentWatts > limit) limit
    else spentWatts - Math.min(refund, spentWatts / 2)
  }
}
