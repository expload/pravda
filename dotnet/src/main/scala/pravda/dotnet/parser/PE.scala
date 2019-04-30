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

package pravda.dotnet.parser

import fastparse.byte.all._
import LE._
import pravda.dotnet.utils._
import pravda.dotnet.utils

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

// See http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf p.271
// II.24 Metadata physical layout

// For portable PDB format see
// https://github.com/dotnet/corefx/blob/master/src/System.Reflection.Metadata/specs/PortablePdb-Metadata.md
object PE {

  object Info {
    final case class PeFileHeader(sectionNumber: Int,
                                  optionHeaderSize: Int /*probably non necessary*/,
                                  characteristics: Short)
    final case class PeHeaderStandardFields(codeSize: Long,
                                            initDataSize: Long,
                                            uninitDataSize: Long,
                                            entryRva: Long,
                                            codeRva: Long,
                                            dataRva: Long)
    final case class NtSpecificFields(imageBase: Long, sectionAligment: Long, imageSize: Long, headerSize: Long)
    final case class PeHeaderDataDirectories(importTableRva: Long,
                                             importTableSize: Long,
                                             baseRelocationTableRva: Long,
                                             baseRelocationBaseSize: Long,
                                             importAddressTableRva: Long,
                                             importAddressTableSize: Long,
                                             cliHeaderRva: Long,
                                             cliHeaderSize: Long)
    final case class SectionHeader(name: String,
                                   virtualSize: Long,
                                   virtualAddress: Long,
                                   sizeOfRawData: Long,
                                   pointerToRawData: Long,
                                   characteristics: Int)

    final case class CliHeader(cb: Long,
                               majorRuntimeVersion: Int,
                               minorRuntimeVersion: Int,
                               metadataRva: Long,
                               metadataSize: Long,
                               flags: Int,
                               entryPointToken: Int,
                               resourcesRva: Long,
                               resourcesSize: Long,
                               strongNameSignatureRva: Long,
                               strongNameSignatureSize: Long,
                               vTableFixupsRva: Long,
                               vTableFixupsSize: Long)

    final case class StreamHeader(offset: Long, size: Long, name: String)

    final case class TildeStream(heapSizes: Byte, tableNumbers: List[Int], sorted: Long, tables: TablesInfo)

    final case class MetadataRoot(version: String, streamHeaders: List[StreamHeader])

    final case class PeHeader(peFileHeader: PeFileHeader,
                              peHeaderStandardFields: PeHeaderStandardFields,
                              ntSpecificFields: NtSpecificFields,
                              peHeaderDataDirectories: PeHeaderDataDirectories,
                              sectionHeaders: List[SectionHeader])

    sealed trait MethodHeader {
      val codeBytes: Bytes
    }
    case object EmptyHeader extends MethodHeader {
      override val codeBytes: Bytes = Bytes.empty
    }
    final case class TinyMethodHeader(codeBytes: Bytes) extends MethodHeader
    final case class FatMethodHeader(flags: Int, size: Int, maxStack: Int, localVarSigTok: Long, codeBytes: Bytes)
        extends MethodHeader

    final case class PeData(sections: List[(SectionHeader, Bytes)],
                            stringHeap: Bytes,
                            userStringHeap: Bytes,
                            blobHeap: Bytes,
                            tableNumbers: List[Int],
                            tables: TablesInfo)
    final case class Pe(peHeader: PeHeader,
                        cliHeader: CliHeader,
                        metadataRoot: MetadataRoot,
                        peData: PeData,
                        methods: List[MethodHeader])
    final case class Pdb(metadataRoot: MetadataRoot, pdbData: PeData)
  }

  import Info._

  private val msDosHeader: P[Long] = {
    val lfanew = UInt32
    P(BS(0x4D, 0x5A, 0x90, 0x00) ~ AnyBytes(56) ~ lfanew ~ AnyBytes(64))
  }

  private val peFileHeader: P[PeFileHeader] = {
    val sectionsNumber = UInt16
    val optionHeaderSize = UInt16
    val characteristics = Int16
    P(
      BS(0x50, 0x45, 0x00, 0x00) ~ BS(0x4C, 0x01) ~
        sectionsNumber ~ AnyBytes(12) ~ optionHeaderSize ~ characteristics).map(PeFileHeader.tupled)
  }

  private val peHeaderStandardFields: P[PeHeaderStandardFields] = {
    val codeSize = UInt32
    val initDataSize = UInt32
    val uninitDataSize = UInt32
    val entryRva = UInt32
    val codeRva = UInt32
    val dataRva = UInt32
    P(BS(0x0B, 0x01) ~ AnyBytes(2) ~ codeSize ~ initDataSize ~ uninitDataSize ~ entryRva ~ codeRva ~ dataRva)
      .map(PeHeaderStandardFields.tupled)
  }

  private val ntSpecificFields: P[NtSpecificFields] = {
    val imageBase = UInt32
    val sectionAligment = UInt32
    val imageSize = UInt32
    val headerSize = UInt32

    P(imageBase ~ sectionAligment ~ AnyBytes(20) ~ imageSize ~ headerSize ~ AnyBytes(32)).map(NtSpecificFields.tupled)
  }

  private val peHeaderDataDirectories: P[PeHeaderDataDirectories] = {
    val importTableRva = UInt32
    val importTableSize = UInt32
    val baseRelocationTableRva = UInt32
    val baseRelocationTableSize = UInt32
    val importAddressTableRva = UInt32
    val importAddressTableSize = UInt32
    val cliHeaderRva = UInt32
    val cliHeaderSize = UInt32

    P(
      AnyBytes(8) ~
        importTableRva ~ importTableSize ~
        AnyBytes(24) ~
        baseRelocationTableRva ~ baseRelocationTableSize ~
        AnyBytes(48) ~
        importAddressTableRva ~ importAddressTableSize ~
        AnyBytes(8) ~
        cliHeaderRva ~ cliHeaderSize ~
        AnyBytes(8)
    ).map(PeHeaderDataDirectories.tupled)
  }

  private val sectionHeader: P[SectionHeader] = {
    val name = utils.nullTerminatedString(8)
    val virtualSize = UInt32
    val virtualAddress = UInt32
    val sizeOfRawData = UInt32
    val pointerToRawData = UInt32
    val characteristics = Int32

    P(name ~ virtualSize ~ virtualAddress ~ sizeOfRawData ~ pointerToRawData ~ AnyBytes(12) ~ characteristics)
      .map(SectionHeader.tupled)
  }

  private val cliHeader: P[CliHeader] = {
    val cb = UInt32
    val majorRuntimeVersion = UInt16
    val minorRuntimeVersion = UInt16
    val metadataRva = UInt32
    val metadataSize = UInt32
    val flags = Int32
    val entryPointToken = Int32
    val resourcesRva = UInt32
    val resourcesSize = UInt32
    val strongNameSignatureRva = UInt32
    val strongNameSignatureSize = UInt32
    val vTableFixupsRva = UInt32
    val vTableFixupsSize = UInt32

    P(
      cb ~
        majorRuntimeVersion ~ minorRuntimeVersion ~
        metadataRva ~ metadataSize ~
        flags ~
        entryPointToken ~
        resourcesRva ~ resourcesSize ~
        strongNameSignatureRva ~ strongNameSignatureSize ~
        AnyBytes(8) ~
        vTableFixupsRva ~ vTableFixupsSize ~
        AnyBytes(16)).map(CliHeader.tupled)
  }

  private val streamHeader: P[StreamHeader] = {
    val offset = UInt32
    val size = UInt32
    val name = for {
      s <- utils.nullTerminatedString
      padding <- if ((s.length + 1) % 4 == 0) Pass else AnyBytes(4 - (s.length + 1) % 4)
    } yield s

    P(offset ~ size ~ name).map(StreamHeader.tupled)
  }

  private val tildeStream: P[Either[String, TildeStream]] = {
    val heapSizes = Int8
    val valid = Int64
    val sorted = Int64

    for {
      _ <- AnyBytes(6)
      hs <- heapSizes
      _ <- AnyByte
      v <- valid
      s <- sorted
      tableNumbers = TablesInfo.validToActualTableNumbers(v)
      rows <- P(UInt32.rep(exactly = tableNumbers.length)).map(_.toList)
      tables <- TablesInfo.tables(hs, tableNumbers, rows)
    } yield tables.map(ts => TildeStream(hs, tableNumbers, s, ts))
  }

  private val metadataRoot: P[MetadataRoot] = {
    val length = Int32
    val version = length.flatMap(l => utils.nullTerminatedString((l + 3) / 4 * 4))
    val streamsNumber = UInt16
    val streamHeaders = streamsNumber.flatMap(l => streamHeader.rep(exactly = l)).map(_.toList)
    P(BS(0x42, 0x53, 0x4a, 0x42) ~ AnyBytes(8) ~ version ~ AnyBytes(2) ~ streamHeaders).map(MetadataRoot.tupled)
  }

  private val method: P[MethodHeader] = {
    P(Int8)
      .flatMap(
        b => {
          (b & 0x3) /*method flags*/ match {
            case 0x2 =>
              AnyBytes((b & 0xff) >> 2).!.map(TinyMethodHeader)
            case 0x3 =>
              val flagsAndSize =
                P(Int8).map(b2 => (b.toInt + ((b2 & 0xf) << 8) /*flags*/, (b2 & 0x0f) >> 4) /*size*/ )
              val maxStack = UInt16
              val codeSize = UInt32
              val localVarSigTok = UInt32

              P(flagsAndSize ~ maxStack ~ codeSize ~ localVarSigTok).flatMap {
                case (f, s, ms, cs, lTok) =>
                  AnyBytes(cs.toInt /* only 2GB again */ ).!.map(bs => FatMethodHeader(f, s, ms, lTok, bs))
              }
          }
        }
      )
  }

  private val peHeader: P[PeHeader] = {
    for {
      offset <- msDosHeader
      fileHeader <- P(AnyBytes((offset - 128).toInt) /* 2GB for .exe file should be enough*/ ~ peFileHeader)
      (sFields, ntFields, dataDirs) <- P(peHeaderStandardFields ~ ntSpecificFields ~ peHeaderDataDirectories)
      sections <- P(sectionHeader.rep(exactly = fileHeader.sectionNumber).map(_.toList))
    } yield PeHeader(fileHeader, sFields, ntFields, dataDirs, sections)
  }

  private def sectionFromHeader(file: Bytes, sectionHeader: SectionHeader): Bytes =
    file.slice(sectionHeader.pointerToRawData, sectionHeader.pointerToRawData + sectionHeader.sizeOfRawData)

  def bytesFromRva(sections: List[(SectionHeader, Bytes)], rva: Long, sizeO: Option[Long] = None): Bytes = {
    val rvaSection = sections.find { case (h, _) => rva >= h.virtualAddress && rva <= h.virtualAddress + h.virtualSize }
    rvaSection match {
      case Some((h, s)) =>
        val start = rva - h.virtualAddress
        sizeO
          .map(size => s.slice(start, start + size))
          .getOrElse(s.drop(start)) // probably should be padded with zeros to virtualSize
      case None => Bytes.empty
    }
  }

  private def streamHeaderBytes(sections: List[(SectionHeader, Bytes)],
                                metadataRva: Long,
                                streamHeader: StreamHeader): Bytes = {
    val rva = metadataRva + streamHeader.offset
    bytesFromRva(sections, rva).take(streamHeader.size)
  }

  def parseInfo(file: Bytes): Either[String, Pe] = {
    for {
      header <- peHeader.parse(file).toEither
      sections = header.sectionHeaders.map(h => (h, sectionFromHeader(file, h)))

      fileBytesFromRva = (rva: Long) => bytesFromRva(sections, rva)

      cliHeader <- cliHeader.parse(fileBytesFromRva(header.peHeaderDataDirectories.cliHeaderRva)).toEither
      metadataRva = cliHeader.metadataRva

      metadata <- metadataRoot.parse(fileBytesFromRva(metadataRva)).toEither
      streamHeaders = metadata.streamHeaders

      retrieveStream = (name: String) =>
        streamHeaders
          .find(_.name == name)
          .map(h => Right(streamHeaderBytes(sections, metadataRva, h)))
          .getOrElse(Left(s"$name heap not found"))

      tildeStreamBytes <- retrieveStream("#~")
      stringHeap <- retrieveStream("#Strings")
      userStringHeap <- retrieveStream("#US")
      blobHeap <- retrieveStream("#Blob")

      tildeStream <- tildeStream.parse(tildeStreamBytes).toEither.joinRight

      methods <- tildeStream.tables.methodDefTable
        .map(m =>
          if (m.rva > 0) {
            method.parse(fileBytesFromRva(m.rva)).toEither
          } else {
            Right(EmptyHeader)
        })
        .toList
        .sequence
    } yield
      Pe(header,
         cliHeader,
         metadata,
         PeData(sections, stringHeap, userStringHeap, blobHeap, tildeStream.tableNumbers, tildeStream.tables),
         methods)
  }

  def parsePdb(file: Bytes): Either[String, Pdb] = {
    for {
      metadata <- metadataRoot.parse(file).toEither
      streamHeaders = metadata.streamHeaders

      retrieveStream = (name: String) =>
        streamHeaders
          .find(_.name == name)
          .map(h => Right(file.slice(h.offset, h.offset + h.size)))
          .getOrElse(Left(s"$name heap not found"))

      tildeStreamBytes <- retrieveStream("#~")
      stringHeap <- retrieveStream("#Strings")
      blobHeap <- retrieveStream("#Blob")

      tildeStream <- tildeStream.parse(tildeStreamBytes).toEither.joinRight
    } yield
      Pdb(
        metadata,
        PeData(
          List.empty,
          stringHeap,
          Bytes.empty,
          blobHeap,
          tildeStream.tableNumbers,
          tildeStream.tables
        )
      )
  }
}
