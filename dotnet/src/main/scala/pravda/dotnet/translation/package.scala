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
