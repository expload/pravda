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
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.Signatures
import pravda.dotnet.parser.Signatures._

package object translation {

  object TypeDetectors {

    object Bytes {

      def unapply(sigType: SigType): Boolean = sigType match {
        case SigType.Cls(TypeRefData(_, "Bytes", "Expload.Pravda")) => true
        case _                                                      => false
      }
    }

    object Mapping {

      def unapply(sigType: SigType): Boolean = sigType match {
        case SigType.Cls(TypeRefData(_, "Mapping`2", "Expload.Pravda")) => true
        case _                                                          => false
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
    private def withMethodRefDef[T](sigFunc: Signature => T): (MethodRefDefData, Map[Long, Signature]) => Option[T] =
      (method, signatures) => signatures.get(method.signatureIdx).map(sigFunc)
    private def withMethodRefDefOpt[T](
        sigFunc: Signature => Option[T]): (MethodRefDefData, Map[Long, Signature]) => Option[T] =
      (m, s) => withMethodRefDef(sigFunc)(m, s).flatten

    def methodType(sig: Signature): Option[SigType] =
      sig match {
        case MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), _) => Some(tpe)
        case _                                              => None
      }

    def methodType(m: MethodRefDefData, signatures: Map[Long, Signature]): Option[SigType] =
      withMethodRefDefOpt(methodType)(m, signatures)

    def methodParams(sig: Signature): Option[List[Tpe]] =
      sig match {
        case MethodRefDefSig(_, _, _, _, 0, _, params) => Some(params)
        case _                                         => None
      }

    def methodParams(m: MethodRefDefData, signatures: Map[Long, Signature]): Option[List[Tpe]] =
      withMethodRefDefOpt(methodParams)(m, signatures)

    def methodParamsCount(sig: Signature): Int =
      methodParams(sig).map(_.length).getOrElse(0)

    def methodParamsCount(m: MethodRefDefData, signatures: Map[Long, Signature]): Int =
      withMethodRefDef(methodParamsCount)(m, signatures).getOrElse(0)

    def returnTpe(sig: Signature): Option[SigType] =
      sig match {
        case MethodRefDefSig(_, _, _, _, _, Tpe(tpe, _), _) => Some(tpe)
        case _                                              => None
      }

    def returnTpe(m: MethodRefDefData, signatures: Map[Long, Signature]): Option[SigType] =
      withMethodRefDefOpt(returnTpe)(m, signatures)

    def isVoid(sig: Signature): Boolean =
      returnTpe(sig).contains(SigType.Void)

    def isVoid(m: MethodRefDefData, signatures: Map[Long, Signature]): Boolean =
      withMethodRefDef(isVoid)(m, signatures).getOrElse(false)

    def isCtor(m: MethodDefData): Boolean =
      m.name == ".ctor" && (m.flags & 0x1800) != 0 // maybe the mask should be different (see 252-nd page in spec)

    def isCctor(m: MethodDefData): Boolean =
      m.name == ".cctor" && (m.flags & 0x1810) != 0 // maybe the mask should be different (see 252-nd page in spec)

    def isMain(m: MethodDefData): Boolean =
      m.name == "Main" && (m.flags & 0x10) != 0

    def isPrivate(m: MethodDefData): Boolean =
      (m.flags & 0x7) == 0x1

    def isPublic(m: MethodDefData): Boolean =
      (m.flags & 0x7) == 0x6

    def isStatic(m: MethodDefData): Boolean =
      (m.flags & 0x10) != 0

    def isVirtual(m: MethodDefData): Boolean =
      (m.flags & 0x40) != 0
  }

  object FieldExtractors {

    def isPrivate(f: FieldData): Boolean =
      (f.flags & 0x7) == 0x1

    def isStatic(flags: Short): Boolean =
      (flags & 0x10) != 0
  }

  object NamesBuilder {

    def fullMethod(name: String, sigO: Option[Signature]): String = {
      val normalizedName = if (name == ".ctor" || name == ".cctor") name.drop(1) else name
      val sigParams = sigO.collect { case m: MethodRefDefSig => m.params }.getOrElse(List.empty)
      if (sigParams.nonEmpty) {
        s"${normalizedName}_${sigParams.map(_.tpe.mkString).mkString("_")}"
      } else {
        normalizedName
      }
    }

    def fullType(namespace: String, name: String): String =
      if (namespace.nonEmpty) {
        s"$namespace.$name"
      } else {
        name
      }

    def fullTypeDef(typeDefData: TypeDefData): String =
      fullType(typeDefData.namespace, typeDefData.name)

    def fullTypeMethod(typeDef: TypeDefData, methodName: String, sig: Option[Signature]): String =
      s"${fullTypeDef(typeDef)}.${fullMethod(methodName, sig)}"
  }
}
