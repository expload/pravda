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
                      typeDefTable: Vector[TablesData.TypeDefData],
                      typeRefTable: Vector[TablesData.TypeRefData],
                      typeSpecTable: Vector[TablesData.TypeSpecData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x7
    val rowIdx = (idx >> 3).toInt - 1
    tableIdx match {
      case 0 => Right(typeDefTable(rowIdx))
      case 1 => Right(typeRefTable(rowIdx))
      case 4 => Right(typeSpecTable(rowIdx))
      case n => Left(s"Unknown MemberRefParent table index: $n")
    }
  }
}
