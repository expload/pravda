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

package pravda.dotnet.parsers

import fastparse.byte.all._
import LE._

final case class TablesInfo(
    customAttributeTable: Vector[TablesInfo.CustomAttributeRow] = Vector.empty,
    fieldTable: Vector[TablesInfo.FieldRow] = Vector.empty,
    fieldRVATable: Vector[TablesInfo.FieldRVARow] = Vector.empty,
    memberRefTable: Vector[TablesInfo.MemberRefRow] = Vector.empty,
    methodDefTable: Vector[TablesInfo.MethodDefRow] = Vector.empty,
    paramTable: Vector[TablesInfo.ParamRow] = Vector.empty,
    typeDefTable: Vector[TablesInfo.TypeDefRow] = Vector.empty,
    typeRefTable: Vector[TablesInfo.TypeRefRow] = Vector.empty,
    typeSpecTable: Vector[TablesInfo.TypeSpecRow] = Vector.empty,
    standAloneSigTable: Vector[TablesInfo.StandAloneSigRow] = Vector.empty,
    documentTable: Vector[TablesInfo.DocumentRow] = Vector.empty,
    methodDebugInformationTable: Vector[TablesInfo.MethodDebugInformationRow] = Vector.empty
)

object TablesInfo {

  trait Index {
    val size: Int
    val parser: P[Long]
  }
  case object ShortIndex extends Index {
    override val size: Int = 2
    override val parser: P[Long] = P(UInt16).map(_.toLong)
  }
  case object LongIndex extends Index {
    override val size: Int = 4
    override val parser: P[Long] = P(UInt32)
  }

  final case class TableIndexes(blobHeap: Index,
                                stringHeap: Index,
                                guidHeap: Index,
                                assemblyRef: Index,
                                typeDef: Index,
                                event: Index,
                                hasConstant: Index,
                                hasCustomAttribute: Index,
                                hasDeclSecurity: Index,
                                hasFieldMarshal: Index,
                                hasSemantics: Index,
                                customAttibuteType: Index,
                                typeDefOrRef: Index,
                                typeOrMethodDef: Index,
                                methodDefOrRef: Index,
                                implementation: Index,
                                importScope: Index,
                                field: Index,
                                genericParam: Index,
                                memberForwarded: Index,
                                moduleRef: Index,
                                memberRefParent: Index,
                                methodDef: Index,
                                param: Index,
                                property: Index,
                                resolutionScope: Index,
                                document: Index,
                                localVariable: Index,
                                localConstant: Index,
                                hasCustomDebugInformation: Index)

  sealed trait TableRowInfo

  // PE tables

  case object AssemblyRow             extends TableRowInfo
  case object AssemblyOSRow           extends TableRowInfo
  case object AssemblyProcessorRow    extends TableRowInfo
  case object AssemblyRefRow          extends TableRowInfo
  case object AssemblyRefOSRow        extends TableRowInfo
  case object AssemblyRefProcessorRow extends TableRowInfo

  case object ClassLayoutRow                                                          extends TableRowInfo
  case object ConstantRow                                                             extends TableRowInfo
  final case class CustomAttributeRow(parentIdx: Long, typeIdx: Long, valueIdx: Long) extends TableRowInfo

  case object DeclSecurityRow extends TableRowInfo

  case object EventMapRow     extends TableRowInfo
  case object EventRow        extends TableRowInfo
  case object ExportedTypeRow extends TableRowInfo

  final case class FieldRow(flags: Short, nameIdx: Long, signatureIdx: Long) extends TableRowInfo
  case object FieldLayoutRow                                                 extends TableRowInfo
  case object FieldMarshalRow                                                extends TableRowInfo
  final case class FieldRVARow(rva: Long, fieldIdx: Long)                    extends TableRowInfo
  case object FileRow                                                        extends TableRowInfo

  case object GenericParamRow           extends TableRowInfo
  case object GenericParamConstraintRow extends TableRowInfo

  case object ImplMapRow       extends TableRowInfo
  case object InterfaceImplRow extends TableRowInfo

  case object ManifestResourceRow                                             extends TableRowInfo
  final case class MemberRefRow(cls: Long, nameIdx: Long, signatureIdx: Long) extends TableRowInfo
  final case class MethodDefRow(rva: Long,
                                implFlags: Short,
                                flags: Short,
                                nameIdx: Long,
                                signatureIdx: Long,
                                paramListIdx: Long)
      extends TableRowInfo
  case object MethodImplRow      extends TableRowInfo
  case object MethodSemanticsRow extends TableRowInfo
  case object MethodSpecRow      extends TableRowInfo
  case object ModuleRow          extends TableRowInfo
  case object ModuleRefRow       extends TableRowInfo

  case object NestedClassRow extends TableRowInfo

  final case class ParamRow(flags: Short, seq: Int, nameIdx: Long) extends TableRowInfo
  case object PropertyRow                                          extends TableRowInfo
  case object PropertyMapRow                                       extends TableRowInfo

  final case class StandAloneSigRow(sigIdx: Long) extends TableRowInfo

  final case class TypeDefRow(flags: Int,
                              nameIdx: Long,
                              namespace: Long,
                              parent: Long,
                              fieldListIdx: Long,
                              methodListIdx: Long)
      extends TableRowInfo

  final case class TypeRefRow(resolutionScopeIdx: Long, nameIdx: Long, namespaceIdx: Long) extends TableRowInfo

  final case class TypeSpecRow(blobIdx: Long) extends TableRowInfo

  // PDB tables

  final case class DocumentRow(nameIdx: Long, hashAlgorithnIdx: Long, hashIdx: Long, languageIdx: Long)
      extends TableRowInfo

  final case class MethodDebugInformationRow(documentIdx: Long, seqPoints: Long) extends TableRowInfo

  case object LocalScopeRow             extends TableRowInfo
  case object LocalVariableRow          extends TableRowInfo
  case object LocalConstantRow          extends TableRowInfo
  case object ImportScopeRow            extends TableRowInfo
  case object StateMachineMethodRow     extends TableRowInfo
  case object CustomDebugInformationRow extends TableRowInfo

  // PE tables

  def assemblyRow(indexes: TableIndexes): P[AssemblyRow.type] =
    P(AnyBytes(4 + 4 * 2 + 4 + indexes.blobHeap.size + indexes.stringHeap.size * 2)).map(_ => AssemblyRow)

  val assemblyOSRow: P[AssemblyOSRow.type] =
    P(AnyBytes(4 * 3)).map(_ => AssemblyOSRow)

  val asssemblyProcessorRow: P[AssemblyProcessorRow.type] =
    P(AnyBytes(4)).map(_ => AssemblyProcessorRow)

  def assemblyRefRow(indexes: TableIndexes): P[AssemblyRefRow.type] =
    P(AnyBytes(4 * 2 + 4 + indexes.blobHeap.size * 2 + indexes.stringHeap.size * 2)).map(_ => AssemblyRefRow)

  def assemblyRefOSRow(indexes: TableIndexes): P[AssemblyRefOSRow.type] =
    P(AnyBytes(3 * 4 + indexes.assemblyRef.size)).map(_ => AssemblyRefOSRow)

  def assemblyRefProcessorRow(indexes: TableIndexes): P[AssemblyRefProcessorRow.type] =
    P(AnyBytes(4 + indexes.assemblyRef.size)).map(_ => AssemblyRefProcessorRow)

  def classLayoutRow(indexes: TableIndexes): P[ClassLayoutRow.type] =
    P(AnyBytes(2 + 4 + indexes.typeDef.size)).map(_ => ClassLayoutRow)

  def constantRow(indexes: TableIndexes): P[ConstantRow.type] =
    P(AnyBytes(2 + indexes.hasConstant.size + indexes.blobHeap.size)).map(_ => ConstantRow)

  def customAttributeRow(indexes: TableIndexes): P[CustomAttributeRow] =
    P(indexes.hasCustomAttribute.parser ~ indexes.customAttibuteType.parser ~ indexes.blobHeap.parser)
      .map(CustomAttributeRow.tupled)

  def declSecurityRow(indexes: TableIndexes): P[DeclSecurityRow.type] =
    P(AnyBytes(2 + indexes.hasDeclSecurity.size + indexes.blobHeap.size)).map(_ => DeclSecurityRow)

  def eventMapRow(indexes: TableIndexes): P[EventMapRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.event.size)).map(_ => EventMapRow)

  def eventRow(indexes: TableIndexes): P[EventRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.typeDefOrRef.size)).map(_ => EventRow)

  def exportedTypeRow(indexes: TableIndexes): P[ExportedTypeRow.type] =
    P(AnyBytes(4 + 4 + indexes.stringHeap.size + indexes.stringHeap.size + indexes.implementation.size))
      .map(_ => ExportedTypeRow)

  def fieldRow(indexes: TableIndexes): P[FieldRow] =
    P(Int16 ~ indexes.stringHeap.parser ~ indexes.blobHeap.parser).map(FieldRow.tupled)

  def fieldLayoutRow(indexes: TableIndexes): P[FieldLayoutRow.type] =
    P(AnyBytes(4 + indexes.field.size)).map(_ => FieldLayoutRow)

  def fieldMarshalRow(indexes: TableIndexes): P[FieldMarshalRow.type] =
    P(AnyBytes(indexes.hasFieldMarshal.size + indexes.blobHeap.size)).map(_ => FieldMarshalRow)

  def fieldRVARow(indexes: TableIndexes): P[FieldRVARow] =
    P(UInt32 ~ indexes.field.parser).map(FieldRVARow.tupled)

  def fileRow(indexes: TableIndexes): P[FileRow.type] =
    P(AnyBytes(4 + indexes.stringHeap.size + indexes.blobHeap.size)).map(_ => FileRow)

  def genericParamRow(indexes: TableIndexes): P[GenericParamRow.type] =
    P(AnyBytes(2 + 2 + indexes.typeOrMethodDef.size + indexes.stringHeap.size)).map(_ => GenericParamRow)

  def genericParamConstraintRow(indexes: TableIndexes): P[GenericParamConstraintRow.type] =
    P(AnyBytes(indexes.genericParam.size + indexes.typeDefOrRef.size)).map(_ => GenericParamConstraintRow)

  def implMapRow(indexes: TableIndexes): P[ImplMapRow.type] =
    P(AnyBytes(2 + indexes.memberForwarded.size + indexes.stringHeap.size + indexes.moduleRef.size))
      .map(_ => ImplMapRow)

  def interfaceImplRow(indexes: TableIndexes): P[InterfaceImplRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.typeDefOrRef.size)).map(_ => InterfaceImplRow)

  def manifestResourceRow(indexes: TableIndexes): P[ManifestResourceRow.type] =
    P(AnyBytes(4 + 4 + indexes.stringHeap.size + indexes.implementation.size)).map(_ => ManifestResourceRow)

  def memberRefRow(indexes: TableIndexes): P[MemberRefRow] =
    P(indexes.memberRefParent.parser ~ indexes.stringHeap.parser ~ indexes.blobHeap.parser).map(MemberRefRow.tupled)

  def methodDefRow(indexes: TableIndexes): P[MethodDefRow] = {
    val rva = UInt32
    val implFlags = Int16
    val flags = Int16
    val nameIdx = indexes.stringHeap.parser
    val signatureIdx = indexes.blobHeap.parser
    val paramListIdx = indexes.param.parser
    P(rva ~ implFlags ~ flags ~ nameIdx ~ signatureIdx ~ paramListIdx).map(MethodDefRow.tupled)
  }

  def methodImplRow(indexes: TableIndexes): P[MethodImplRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.methodDefOrRef.size * 2)).map(_ => MethodImplRow)

  def methodSemanticsRow(indexes: TableIndexes): P[MethodSemanticsRow.type] =
    P(AnyBytes(2 + indexes.methodDef.size + indexes.hasSemantics.size)).map(_ => MethodSemanticsRow)

  def methodSpecRow(indexes: TableIndexes): P[MethodSpecRow.type] =
    P(AnyBytes(indexes.methodDefOrRef.size + indexes.blobHeap.size)).map(_ => MethodSpecRow)

  def moduleRow(indexes: TableIndexes): P[ModuleRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.guidHeap.size * 3)).map(_ => ModuleRow)

  def moduleRefRow(indexes: TableIndexes): P[ModuleRefRow.type] =
    P(AnyBytes(indexes.stringHeap.size)).map(_ => ModuleRefRow)

  def nestedClassRow(indexes: TableIndexes): P[NestedClassRow.type] =
    P(AnyBytes(indexes.typeDef.size * 2)).map(_ => NestedClassRow)

  def paramRow(indexes: TableIndexes): P[ParamRow] =
    P(Int16 ~ UInt16 ~ indexes.stringHeap.parser).map(ParamRow.tupled)

  def propertyRow(indexes: TableIndexes): P[PropertyRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.blobHeap.size)).map(_ => PropertyRow)

  def propertyMapRow(indexes: TableIndexes): P[PropertyMapRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.property.size)).map(_ => PropertyMapRow)

  def standAloneSigRow(indexes: TableIndexes): P[StandAloneSigRow] =
    P(indexes.blobHeap.parser).map(StandAloneSigRow)

  def typeDefRow(indexes: TableIndexes): P[TypeDefRow] =
    P(Int32 ~ indexes.stringHeap.parser ~ indexes.stringHeap.parser ~ indexes.typeDefOrRef.parser ~ indexes.field.parser ~ indexes.methodDef.parser)
      .map(TypeDefRow.tupled)

  def typeRefRow(indexes: TableIndexes): P[TypeRefRow] =
    P(indexes.resolutionScope.parser ~ indexes.stringHeap.parser ~ indexes.stringHeap.parser).map(TypeRefRow.tupled)

  def typeSpecRow(indexes: TableIndexes): P[TypeSpecRow] =
    P(indexes.blobHeap.parser).map(TypeSpecRow)

  // PDB tables

  def documentRow(indexes: TableIndexes): P[DocumentRow] =
    P(indexes.blobHeap.parser ~ indexes.guidHeap.parser ~ indexes.blobHeap.parser ~ indexes.guidHeap.parser)
      .map(DocumentRow.tupled)

  def methodDebugInformationRow(indexes: TableIndexes): P[MethodDebugInformationRow] =
    P(indexes.document.parser ~ indexes.blobHeap.parser).map(MethodDebugInformationRow.tupled)

  def localScopeRow(indexes: TableIndexes): P[LocalScopeRow.type] =
    P(AnyBytes(
      indexes.methodDef.size + indexes.importScope.size + indexes.localVariable.size + indexes.localConstant.size + 4 + 4))
      .map(_ => LocalScopeRow)

  def localVariableRow(indexes: TableIndexes): P[LocalVariableRow.type] =
    P(AnyBytes(2 + 2 + indexes.stringHeap.size)).map(_ => LocalVariableRow)

  def localConstantRow(indexes: TableIndexes): P[LocalConstantRow.type] =
    P(AnyBytes(indexes.stringHeap.size + indexes.blobHeap.size)).map(_ => LocalConstantRow)

  def importScopeRow(indexes: TableIndexes): P[ImportScopeRow.type] =
    P(AnyBytes(indexes.importScope.size + indexes.blobHeap.size)).map(_ => ImportScopeRow)

  def stateMachineMethoRow(indexes: TableIndexes): P[StateMachineMethodRow.type] =
    P(AnyBytes(indexes.methodDef.size + indexes.methodDef.size)).map(_ => StateMachineMethodRow)

  def customDebugInformationRow(indexes: TableIndexes): P[CustomDebugInformationRow.type] =
    P(AnyBytes(indexes.hasCustomDebugInformation.size + indexes.guidHeap.size + indexes.blobHeap.size)).map(_ =>
      CustomDebugInformationRow)

  def tableParser(num: Int, row: Long, indexes: TableIndexes): P[Either[String, TablesInfo => TablesInfo]] = {
    def tableRep[T](p: TableIndexes => P[T]): P[Vector[T]] =
      p(indexes).rep(exactly = row.toInt).map(_.toVector)

    def tablesId(p: TableIndexes => P[TableRowInfo]): P[Either[String, TablesInfo => TablesInfo]] =
      tableRep(p).map(r => Right(t => t))

    num match {
      case 0 => tablesId(moduleRow)
      case 1 => tableRep(typeRefRow).map(r => Right(_.copy(typeRefTable = r)))
      case 2 => tableRep(typeDefRow).map(r => Right(_.copy(typeDefTable = r)))
      // 3
      case 4 => tableRep(fieldRow).map(r => Right(_.copy(fieldTable = r)))
      // 5
      case 6 => tableRep(methodDefRow).map(r => Right(_.copy(methodDefTable = r)))
      // 7
      case 8  => tableRep(paramRow).map(r => Right(_.copy(paramTable = r)))
      case 9  => tablesId(interfaceImplRow)
      case 10 => tableRep(memberRefRow).map(r => Right(_.copy(memberRefTable = r)))
      case 11 => tablesId(constantRow)
      case 12 => tableRep(customAttributeRow).map(r => Right(_.copy(customAttributeTable = r)))
      case 13 => tablesId(fieldMarshalRow)
      case 14 => tablesId(declSecurityRow)
      case 15 => tablesId(classLayoutRow)
      case 16 => tablesId(fieldLayoutRow)
      case 17 => tableRep(standAloneSigRow).map(r => Right(_.copy(standAloneSigTable = r)))
      case 18 => tablesId(eventMapRow)
      // 19
      case 20 => tablesId(eventRow)
      case 21 => tablesId(propertyMapRow)
      // 22
      case 23 => tablesId(propertyRow)
      case 24 => tablesId(methodSemanticsRow)
      case 25 => tablesId(methodImplRow)
      case 26 => tablesId(moduleRefRow)
      case 27 => tableRep(typeSpecRow).map(r => Right(_.copy(typeSpecTable = r)))
      case 28 => tablesId(implMapRow)
      case 29 => tableRep(fieldRVARow).map(r => Right(_.copy(fieldRVATable = r)))
      // 30
      // 31
      case 32 => tablesId(assemblyRow)
      case 33 => tablesId(_ => asssemblyProcessorRow)
      case 34 => tablesId(_ => assemblyOSRow)
      case 35 => tablesId(assemblyRefRow)
      case 36 => tablesId(assemblyRefProcessorRow)
      case 37 => tablesId(assemblyRefOSRow)
      case 38 => tablesId(fileRow)
      case 39 => tablesId(exportedTypeRow)
      case 40 => tablesId(manifestResourceRow)
      case 41 => tablesId(nestedClassRow)
      case 42 => tablesId(genericParamRow)
      case 43 => tablesId(methodSpecRow)
      case 44 => tablesId(genericParamConstraintRow)
      // 45
      // 46
      // 47
      case 48 => tableRep(documentRow).map(r => Right(_.copy(documentTable = r)))
      case 49 => tableRep(methodDebugInformationRow).map(r => Right(_.copy(methodDebugInformationTable = r)))
      case 50 => tablesId(localScopeRow)
      case 51 => tablesId(localVariableRow)
      case 52 => tablesId(localConstantRow)
      case 53 => tablesId(importScopeRow)
      case 54 => tablesId(stateMachineMethoRow)
      case 55 => tablesId(customDebugInformationRow)
      case n  => PassWith(Left(s"Non valid table number: $n"))
    }
  }

  def validToActualTableNumbers(valid: Long): List[Int] =
    valid.toBinaryString.reverse.zipWithIndex.filter(_._1 == '1').map(_._2).toList

  private def tableIndexes(heapSizes: Byte, tableNumbers: List[Int], rows: List[Long]): TableIndexes = {
    TableIndexes(
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex
    ) // FIXME
  }

  def tables(heapSizes: Byte, tableNumbers: List[Int], rows: List[Long]): P[Either[String, TablesInfo]] = {
    val indexes = tableIndexes(heapSizes, tableNumbers, rows)

    rows
      .zip(tableNumbers)
      .map {
        case (row, num) => tableParser(num, row, indexes)
      }
      .foldLeft[P[Either[String, TablesInfo]]](PassWith(Right(TablesInfo()))) {
        case (tablesP, tablesPT) =>
          for {
            tablesE <- tablesP
            tablesTE <- tablesPT
          } yield
            for {
              tables <- tablesE
              tablesT <- tablesTE
            } yield tablesT(tables)
      }
  }
}
