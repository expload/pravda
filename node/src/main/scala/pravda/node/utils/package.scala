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
