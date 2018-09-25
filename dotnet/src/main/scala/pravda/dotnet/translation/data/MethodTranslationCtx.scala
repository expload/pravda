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

package pravda.dotnet.translation.data

import pravda.dotnet.data.TablesData
import pravda.dotnet.data.TablesData.{MethodDebugInformationData, TypeDefData}
import pravda.dotnet.parser.CIL.CilData
import pravda.dotnet.parser.Signatures

final case class TranslationCtx(
    signatures: Map[Long, Signatures.Signature],
    cilData: CilData,
    mainProgramClass: TypeDefData,
    programClasses: List[TypeDefData],
    methodsToTypes: Map[Int, TypeDefData],
    pdbTables: Option[TablesData]
) {

  def tpeByMethodDef(m: TablesData.MethodDefData): Option[TypeDefData] = {
    val idx = cilData.tables.methodDefTable.indexWhere(_ == m)
    if (idx == -1) {
      None
    } else {
      methodsToTypes.get(idx)
    }
  }

  def isMainProgramMethod(idx: Int): Boolean =
    methodsToTypes.get(idx).contains(mainProgramClass)

  def isMainProgramMethod(m: TablesData.MethodDefData): Boolean = {
    val idx = cilData.tables.methodDefTable.indexWhere(_ == m)
    if (idx == -1) {
      false
    } else {
      isMainProgramMethod(idx)
    }
  }

  def isProgramMethod(idx: Int): Boolean =
    methodsToTypes.get(idx).exists(programClasses.contains)

  def isProgramMethod(m: TablesData.MethodDefData): Boolean = {
    val idx = cilData.tables.methodDefTable.indexWhere(_ == m)
    if (idx == -1) {
      false
    } else {
      isProgramMethod(idx)
    }
  }

  def methodRow(idx: Int): TablesData.MethodDefData = cilData.tables.methodDefTable(idx)
}

final case class MethodTranslationCtx(
    tctx: TranslationCtx,
    argsCount: Int,
    localsCount: Int,
    name: String,
    kind: String,
    void: Boolean,
    func: Boolean,
    static: Boolean,
    struct: Option[String],
    debugInfo: Option[MethodDebugInformationData]
)
