package pravda.node

import com.google.protobuf.ByteString

import scala.concurrent.Future

import pravda.common.bytes.byteString2hex

package object utils {

  def showStack(stack: Seq[ByteString]): String =
    stack
      .map(bs => '"' + byteString2hex(bs) + '"')
      .mkString("[", ", ", "]")

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
