package pravda.dotnet.translation.opcode

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._

object OpcodeDetectors {

  object IntLoad {

    def unapply(op: CIL.Op): Option[Int] = op match {
      case LdcI40      => Some(0)
      case LdcI41      => Some(1)
      case LdcI42      => Some(2)
      case LdcI43      => Some(3)
      case LdcI44      => Some(4)
      case LdcI45      => Some(5)
      case LdcI46      => Some(6)
      case LdcI47      => Some(7)
      case LdcI48      => Some(8)
      case LdcI4M1     => Some(-1)
      case LdcI4(num)  => Some(num)
      case LdcI4S(num) => Some(num.toInt)
      case _           => None
    }
  }
}
