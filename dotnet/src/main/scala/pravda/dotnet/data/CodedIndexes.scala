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

package pravda.dotnet.data

import pravda.dotnet.data.TablesData._

object CodedIndexes {

  def memberRefParent(idx: Long,
                      typeDefTable: Vector[TypeDefData],
                      typeRefTable: Vector[TypeRefData],
                      typeSpecTable: Vector[TypeSpecData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x7
    val rowIdx = (idx >> 3).toInt - 1
    tableIdx match {
      case 0     => Right(typeDefTable(rowIdx))
      case 1     => Right(typeRefTable(rowIdx))
      case 4     => Right(typeSpecTable(rowIdx))
      case 2 | 3 => Right(Ignored)
      case n     => Left(s"Unknown MemberRefParent table index: $n")
    }
  }

  def typeDefOrRef(idx: Long,
                   typeDefTable: Vector[TypeDefData],
                   typeRefTable: Vector[TypeRefData],
                   typeSpecTable: Vector[TypeSpecData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x3
    val rowIdx = (idx >> 2).toInt - 1
    if (rowIdx == -1) {
      Right(Ignored)
    } else {
      tableIdx match {
        case 0 => Right(typeDefTable(rowIdx))
        case 1 => Right(typeRefTable(rowIdx))
        case 2 => Right(typeSpecTable(rowIdx))
        case n => Left(s"Unknown TypeDefOrRef table index: $n")
      }
    }
  }

  def methodDefOrRef(idx: Long,
                     methodDefTable: Vector[MethodDefData],
                     memberRefTable: Vector[MemberRefData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x1
    val rowIdx = (idx >> 1).toInt - 1
    if (rowIdx == -1) {
      Right(Ignored)
    } else {
      tableIdx match {
        case 0 => Right(methodDefTable(rowIdx))
        case 1 => Right(memberRefTable(rowIdx))
        case n => Left(s"Unknown TypeDefOrRef table index: $n")
      }
    }
  }

  def hasCustomAttribute(idx: Long, typeDefTable: Vector[TypeDefData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x1f
    val rowIdx = (idx >> 5).toInt - 1
    tableIdx match {
      case 3                      => Right(typeDefTable(rowIdx))
      case n if n >= 0 && n <= 21 => Right(Ignored)
      case n                      => Left(s"Unknown HasCustomAttribute table index: $n")
      // we need information only about [Program] attribute on classes
    }
  }

  def customAttributeType(idx: Long,
                          methodDefTable: Vector[MethodDefData],
                          memberRefTable: Vector[MemberRefData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x7
    val rowIdx = (idx >> 3).toInt - 1
    tableIdx match {
      case 2         => Right(methodDefTable(rowIdx))
      case 3         => Right(memberRefTable(rowIdx))
      case 0 | 1 | 4 => Right(Ignored)
      case n         => Left(s"Unknown CustomAttributeType table index: $n")
    }
  }
}
