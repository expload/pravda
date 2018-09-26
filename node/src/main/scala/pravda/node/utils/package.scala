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

package pravda.node

import pravda.node.data.blockchain.ExecutionInfo
import pravda.vm.Data

import scala.concurrent.Future

package object utils {

  def showExecInfo(info: ExecutionInfo): String = {
    s"""Status: ${info.status}
       |Watts: total ${info.totalWatts}, spent ${info.spentWatts}, refund: ${info.refundWatts}
       |Stack:
       |${showStack(info.stack)}
       |Heap:
       |${showHeap(info.heap)}""".stripMargin
  }

  def showHeap(heap: Seq[Data]): String =
    heap.zipWithIndex
      .map { case (data, i) => s"  ${i.toHexString}: ${data.mkString(pretty = true)}" }
      .mkString("\n")

  def showStack(stack: Seq[Data]): String =
    stack
      .map(data => "  " + data.mkString(pretty = true))
      .mkString("\n")

  def detectCpuArchitecture(): Future[CpuArchitecture] = Future.successful {
    System.getProperty("os.arch").toLowerCase match {
      case s if s.contains("amd64")  => CpuArchitecture.x86_64
      case s if s.contains("x86_64") => CpuArchitecture.x86_64
      case s if s.contains("x86")    => CpuArchitecture.x86
      case _                         => CpuArchitecture.Unsupported
    }
  }

  def detectOperationSystem(): Future[OperationSystem] = Future.successful {
    println(s"System.getProperty(os.name)=${System.getProperty("os.name")}")
    System.getProperty("os.name").toLowerCase match {
      case s if s.contains("mac")   => OperationSystem.MacOS
      case s if s.contains("linux") => OperationSystem.Linux
      case s if s.contains("win")   => OperationSystem.Windows
      case _                        => OperationSystem.Unsupported
    }
  }

}
