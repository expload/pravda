package pravda.dotnet.translation.opcode

import pravda.dotnet.data.TablesData.TypeRefData
import pravda.dotnet.parsers.Signatures.SigType

object TypeDetectors {

  object Address {

    def unapply(sigType: SigType): Boolean = sigType match {
      case SigType.Cls(TypeRefData(_, "Address", "Com.Expload")) => true
      case _                                                     => false
    }
  }

  object Mapping {

    def unapply(sigType: SigType): Boolean = sigType match {
      case SigType.Cls(TypeRefData(_, "Mapping`2", "Com.Expload")) => true
      case _                                                       => false
    }
  }
}
