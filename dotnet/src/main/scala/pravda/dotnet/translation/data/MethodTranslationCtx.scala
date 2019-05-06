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
import pravda.dotnet.data.TablesData.{FieldData, MethodDebugInformationData, MethodDefData, TypeDefData}
import pravda.dotnet.parser.CIL.CilData
import pravda.dotnet.parser.Signatures

/**
  * Context for translation in one file
  *
  * @param signatures all CIL signatures in the file
  * @param cilData all other information from the file
  * @param mainProgramClass main [Program] class
  * @param programClasses all [Program] classes
  * @param structs all non [Program] classes
  * @param methodIndex inverted index for searching for methods
  * @param fieldIndex inverted index for searching for fields
  * @param pdbTables optional CIL tables from .pdb file
  */
final case class TranslationCtx(
    signatures: Map[Long, Signatures.Signature],
    cilData: CilData,
    mainProgramClass: TypeDefData,
    programClasses: List[TypeDefData],
    structs: List[TypeDefData],
    methodIndex: TypeDefInvertedFileIndex[MethodDefData],
    fieldIndex: TypeDefInvertedFileIndex[FieldData],
    pdbTables: Option[TablesData]
) {

  def fieldParent(fileIdx: Int): Option[TypeDefData] = fieldIndex.parent(fileIdx)

  def isMainProgramMethod(fileIdx: Int): Boolean =
    methodIndex.parent(fileIdx).contains(mainProgramClass)

  def isProgramMethod(fileIdx: Int): Boolean =
    methodIndex.parent(fileIdx).exists(programClasses.contains)

  def methodRow(fileIdx: Int): TablesData.MethodDefData = cilData.tables.methodDefTable(fileIdx)
}

/**
  * Context for translation in one method
  *
  * @param tctx        context for translation in the file where the method is sutiated
  * @param argsCount   count of method arguments
  * @param localsCount count of method local variables
  * @param name        name of the method
  * @param kind        special prefix needed to distinguish different kinds of methods
  * @param void        is method void
  * @param func is method "program function".
  *             It means it doesn't have name of method after its arguments on the stack as "program method".
  * @param static is method static
  * @param struct name of struct that contains the method if such struct exists
  * @param debugInfo info about debug symbols
  */
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
