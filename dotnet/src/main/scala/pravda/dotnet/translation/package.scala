package pravda.dotnet

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData.{MethodDefData, TypeRefData}
import pravda.dotnet.parsers.Signatures
import pravda.dotnet.parsers.Signatures.{LocalVarSig, MethodRefDefSig, SigType, Tpe}

package object translation {

  object TypeDetectors {

    object Bytes {

      def unapply(sigType: SigType): Boolean = sigType match {
        case SigType.Cls(TypeRefData(_, "Bytes", "Com.Expload")) => true
        case _                                                   => false
      }
    }

    object Mapping {

      def unapply(sigType: SigType): Boolean = sigType match {
        case SigType.Cls(TypeRefData(_, "Mapping`2", "Com.Expload")) => true
        case _                                                       => false
      }
    }
  }

  object MethodExtractors {

    def localVariables(m: Method, signatures: Map[Long, Signatures.Signature]): Option[List[Signatures.LocalVar]] = {
      val localVarSig = m.localVarSigIdx.flatMap(signatures.get)
      localVarSig
        .map {
          case LocalVarSig(types) => types
          case _                  => List.empty
        }
    }

    def returnTpe(m: MethodDefData, signatures: Map[Long, Signatures.Signature]): Option[SigType] =
      m match {
        case MethodDefData(_, _, _, _, sigIdx, _) =>
          signatures.get(sigIdx) match {
            case Some(MethodRefDefSig(_, _, _, _, _, Tpe(tpe, _), _)) => Some(tpe)
            case _                                                    => None
          }
      }

    def isVoid(m: MethodDefData, signatures: Map[Long, Signatures.Signature]): Boolean =
      returnTpe(m, signatures).contains(SigType.Void)
  }
}
