package pravda.dotnet.translation.opcode

import pravda.dotnet.data.TablesData.TypeDefData
import pravda.dotnet.parsers.Signatures.SigType

object TypeDetectors {

  object Address {

    def unapply(sigType: SigType): Boolean = sigType match {
      case SigType.Cls(TypeDefData(_, "Address", "io.mytc.pravda", _, _, _)) => true
      case _                                                                 => false
    }
  }

  object Mapping {

    def unapply(sigType: SigType): Boolean = sigType match {
      case SigType.Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)) => true
      case _                                                                   => false
    }
  }
}
