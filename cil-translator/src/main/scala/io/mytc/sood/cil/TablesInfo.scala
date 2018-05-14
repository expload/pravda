package io.mytc.sood.cil

import fastparse.byte.all._
import LE._
import io.mytc.sood.cil.utils._

final case class TablesInfo(
    fieldTable: Seq[TablesInfo.FieldRow] = Seq.empty,
    memberRefTable: Seq[TablesInfo.MemberRefRow] = Seq.empty,
    methodDefTable: Seq[TablesInfo.MethodDefRow] = Seq.empty,
    paramTable: Seq[TablesInfo.ParamRow] = Seq.empty,
    typeDefTable: Seq[TablesInfo.TypeDefRow] = Seq.empty
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
                                customAttibute: Index,
                                typeDefOrRef: Index,
                                typeOrMethodDef: Index,
                                methodDefOrRef: Index,
                                implementation: Index,
                                field: Index,
                                genericParam: Index,
                                memberForwarded: Index,
                                moduleRef: Index,
                                memberRefParent: Index,
                                methodDef: Index,
                                param: Index,
                                property: Index,
                                resolutionScope: Index)

  sealed trait TableRowInfo
  case object AssemblyRow extends TableRowInfo
  case object AssemblyOSRow extends TableRowInfo
  case object AssemblyProcessorRow extends TableRowInfo
  case object AssemblyRefRow extends TableRowInfo
  case object AssemblyRefOSRow extends TableRowInfo
  case object AssemblyRefProcessorRow extends TableRowInfo

  case object ClassLayoutRow extends TableRowInfo
  case object ConstantRow extends TableRowInfo
  case object CustomAttributeRow extends TableRowInfo

  case object DeclSecurityRow extends TableRowInfo

  case object EventMapRow extends TableRowInfo
  case object EventRow extends TableRowInfo
  case object ExportedTypeRow extends TableRowInfo

  final case class FieldRow(flags: Short, nameIdx: Long, signatureIdx: Long) extends TableRowInfo
  case object FieldLayoutRow extends TableRowInfo
  case object FieldMarshalRow extends TableRowInfo
  case object FieldRVARow extends TableRowInfo
  case object FileRow extends TableRowInfo

  case object GenericParamRow extends TableRowInfo
  case object GenericParamConstraintRow extends TableRowInfo

  case object ImplMapRow extends TableRowInfo
  case object InterfaceImplRow extends TableRowInfo

  case object ManifestResourceRow extends TableRowInfo
  final case class MemberRefRow(cls: Long, nameIdx: Long, signatureIdx: Long) extends TableRowInfo
  final case class MethodDefRow(rva: Long,
                                implFlags: Short,
                                flags: Short,
                                nameIdx: Long,
                                signatureIdx: Long,
                                paramListIdx: Long)
      extends TableRowInfo
  case object MethodImplRow extends TableRowInfo
  case object MethodSemanticsRow extends TableRowInfo
  case object MethodSpecRow extends TableRowInfo
  case object ModuleRow extends TableRowInfo
  case object ModuleRefRow extends TableRowInfo

  case object NestedClassRow extends TableRowInfo

  final case class ParamRow(flags: Short, seq: Int, nameIdx: Long) extends TableRowInfo
  case object PropertyRow extends TableRowInfo
  case object PropertyMapRow extends TableRowInfo

  case object StandAloneSigRow extends TableRowInfo

  final case class TypeDefRow(flags: Int,
                              nameIdx: Long,
                              namespace: Long,
                              parent: Long,
                              fieldListIdx: Long,
                              methodListIdx: Long)
      extends TableRowInfo
  case object TypeRefRow extends TableRowInfo
  case object TypeSpecRow extends TableRowInfo

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
  def customAttributeRow(indexes: TableIndexes): P[CustomAttributeRow.type] =
    P(AnyBytes(indexes.hasCustomAttribute.size + indexes.customAttibute.size + indexes.blobHeap.size))
      .map(_ => CustomAttributeRow)

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
  def fieldRVARow(indexes: TableIndexes): P[FieldRVARow.type] =
    P(AnyBytes(4 + indexes.field.size)).map(_ => FieldRVARow)
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

  def standAloneSigRow(indexes: TableIndexes): P[StandAloneSigRow.type] =
    P(AnyBytes(indexes.blobHeap.size)).map(_ => StandAloneSigRow)

  def typeDefRow(indexes: TableIndexes): P[TypeDefRow] =
    P(Int32 ~ indexes.stringHeap.parser ~ indexes.stringHeap.parser ~ indexes.typeDefOrRef.parser ~ indexes.field.parser ~ indexes.methodDef.parser)
      .map(TypeDefRow.tupled)
  def typeRefRow(indexes: TableIndexes): P[TypeRefRow.type] =
    P(AnyBytes(indexes.resolutionScope.size + indexes.stringHeap.size * 2)).map(_ => TypeRefRow)
  def typeSpecRow(indexes: TableIndexes): P[TypeSpecRow.type] =
    P(AnyBytes(indexes.blobHeap.size)).map(_ => TypeSpecRow)

  def tableParser(num: Int, row: Long, indexes: TableIndexes): P[Validated[TablesInfo => TablesInfo]] = {
    def tableRep[T](p: TableIndexes => P[T]): P[Seq[T]] =
      p(indexes).rep(exactly = row.toInt)

    def tablesId(p: TableIndexes => P[TableRowInfo]): P[Validated[TablesInfo => TablesInfo]] =
      tableRep(p).map(r => validated(t => t))

    num match {
      case 0 => tablesId(moduleRow)
      case 1 => tablesId(typeRefRow)
      case 2 => tableRep(typeDefRow).map(r => validated(t => t.copy(typeDefTable = r)))
      // 3
      case 4 => tableRep(fieldRow).map(r => validated(t => t.copy(fieldTable = r)))
      // 5
      case 6 => tableRep(methodDefRow).map(r => validated(t => t.copy(methodDefTable = r)))
      // 7
      case 8  => tableRep(paramRow).map(r => validated(t => t.copy(paramTable = r)))
      case 9  => tablesId(interfaceImplRow)
      case 10 => tableRep(memberRefRow).map(r => validated(t => t.copy(memberRefTable = r)))
      case 11 => tablesId(constantRow)
      case 12 => tablesId(customAttributeRow)
      case 13 => tablesId(fieldMarshalRow)
      case 14 => tablesId(declSecurityRow)
      case 15 => tablesId(classLayoutRow)
      case 16 => tablesId(fieldLayoutRow)
      case 17 => tablesId(standAloneSigRow)
      case 18 => tablesId(eventMapRow)
      // 19
      case 20 => tablesId(eventRow)
      case 21 => tablesId(propertyMapRow)
      // 22
      case 23 => tablesId(propertyRow)
      case 24 => tablesId(methodSemanticsRow)
      case 25 => tablesId(methodImplRow)
      case 26 => tablesId(moduleRefRow)
      case 27 => tablesId(typeSpecRow)
      case 28 => tablesId(implMapRow)
      case 29 => tablesId(fieldRVARow)
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
      case n  => PassWith(validationError(s"Non valid table number: $n"))
    }
  }

  def validToActualTableNumbers(valid: Long): Seq[Int] =
    valid.toBinaryString.reverse.zipWithIndex.filter(_._1 == '1').map(_._2)

  private def tableIndexes(heapSizes: Byte, tableNumbers: Seq[Int], rows: Seq[Long]): TableIndexes = {
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
      ShortIndex
    ) // FIXME
  }

  def tables(heapSizes: Byte, tableNumbers: Seq[Int], rows: Seq[Long]): P[Validated[TablesInfo]] = {
    val indexes = tableIndexes(heapSizes, tableNumbers, rows)

    rows
      .zip(tableNumbers)
      .map {
        case (row, num) => tableParser(num, row, indexes)
      }
      .foldLeft[P[Validated[TablesInfo]]](PassWith(validated(TablesInfo()))) {
        case (tablesP, tablesPT) => tablesP.flatMap(tables => tablesPT.map(_.map(t => t(tables))))
      }
  }
}
