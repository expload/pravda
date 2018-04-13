package io.mytc.sood.cil

import fastparse.byte.all._
import LE._
import fastparse.core.Parsed._

// See
//   http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf
//   page 303
object PE {

  case class PeFileHeader(sectionNumber: Int, optionHeaderSize: Int /*probably non necessary*/, characteristics: Short)
  case class PeHeaderStandardFields(codeSize: Long,
                                    initDataSize: Long,
                                    uninitDataSize: Long,
                                    entryRva: Long,
                                    codeRva: Long,
                                    dataRva: Long)
  case class NtSpecificFields(imageBase: Long, sectionAligment: Long, imageSize: Long, headerSize: Long)
  case class PeHeaderDataDirectories(importTableRva: Long,
                                     importTableSize: Long,
                                     baseRelocationTableRva: Long,
                                     baseRelocationBaseSize: Long,
                                     importAddressTableRva: Long,
                                     importAddressTableSize: Long,
                                     cliHeaderRva: Long,
                                     cliHeaderSize: Long)
  case class SectionHeader(name: String,
                           virtualSize: Long,
                           virtualAddress: Long,
                           sizeOfRawData: Long,
                           pointerToRawData: Long,
                           characteristics: Int)

  case class CliHeader(cb: Long,
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

  case class StreamHeader(offset: Long, size: Long, name: String)

  case class TildeStream(heapSizes: Byte, valid: Long, sorted: Long, tables: Seq[Seq[Tables.Info.TableRowInfo]])

  case class MetadataRoot(version: String, streamHeaders: Seq[StreamHeader])

  case class PeHeader(peFileHeader: PeFileHeader,
                      peHeaderStandardFields: PeHeaderStandardFields,
                      ntSpecificFields: NtSpecificFields,
                      peHeaderDataDirectories: PeHeaderDataDirectories,
                      sectionHeaders: Seq[SectionHeader])

  case class Pe(peHeader: PeHeader, cliHeader: CliHeader, metadataRoot: MetadataRoot, tildeStream: TildeStream)

  private def nullTerminatedString(len: Int): P[String] =
    AnyBytes(len).!.map(bs => new String(bs.takeWhile(_ != 0).toArray))

  private val nullTerminatedString: P[String] =
    P(BytesWhile(_ != 0, min = 0).! ~ BS(0)).map(bs => new String(bs.toArray))

  val msDosHeader: P[Long] = {
    val lfanew = UInt32
    P(BS(0x4D, 0x5A, 0x90, 0x00) ~ AnyBytes(56) ~ lfanew ~ AnyBytes(64))
  }

  val peFileHeader: P[PeFileHeader] = {
    val sectionsNumber = UInt16
    val optionHeaderSize = UInt16
    val characteristics = Int16
    P(
      BS(0x50, 0x45, 0x00, 0x00) ~ BS(0x4C, 0x01) ~
        sectionsNumber ~ AnyBytes(12) ~ optionHeaderSize ~ characteristics).map(PeFileHeader.tupled)
  }

  val peHeaderStandardFields: P[PeHeaderStandardFields] = {
    val codeSize = UInt32
    val initDataSize = UInt32
    val uninitDataSize = UInt32
    val entryRva = UInt32
    val codeRva = UInt32
    val dataRva = UInt32
    P(BS(0x0B, 0x01) ~ AnyBytes(2) ~ codeSize ~ initDataSize ~ uninitDataSize ~ entryRva ~ codeRva ~ dataRva)
      .map(PeHeaderStandardFields.tupled)
  }

  val ntSpecificFields: P[NtSpecificFields] = {
    val imageBase = UInt32
    val sectionAligment = UInt32
    val imageSize = UInt32
    val headerSize = UInt32

    P(imageBase ~ sectionAligment ~ AnyBytes(20) ~ imageSize ~ headerSize ~ AnyBytes(32)).map(NtSpecificFields.tupled)
  }

  val peHeaderDataDirectories: P[PeHeaderDataDirectories] = {
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

  val sectionHeader: P[SectionHeader] = {
    val name = nullTerminatedString(8)
    val virtualSize = UInt32
    val virtualAddress = UInt32
    val sizeOfRawData = UInt32
    val pointerToRawData = UInt32
    val characteristics = Int32

    P(name ~ virtualSize ~ virtualAddress ~ sizeOfRawData ~ pointerToRawData ~ AnyBytes(12) ~ characteristics)
      .map(SectionHeader.tupled)
  }

  val cliHeader: P[CliHeader] = {
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

  val streamHeader: P[StreamHeader] = {
    val offset = UInt32
    val size = UInt32
    val name = for {
      s <- nullTerminatedString
      padding <- if ((s.length + 1) % 4 == 0) Pass else AnyBytes(4 - (s.length + 1) % 4)
    } yield s

    P(offset ~ size ~ name).map(StreamHeader.tupled)
  }

  val tildeStream: P[TildeStream] = {
    def bitsCount(x: Long): Int = {
      var i = 0
      var cnt = 0
      while (i < 64) {
        cnt += (if ((x & (1L << i)) != 0) 1 else 0)
        i += 1
      }
      cnt
    }

    val heapSizes = Int8
    val valid = Int64
    val sorted = Int64

    for {
      _ <- AnyBytes(6)
      hs <- heapSizes
      _ <- AnyByte
      v <- valid
      s <- sorted
      n = bitsCount(v)
      rows <- P(UInt32.rep(exactly = n))
      tables <- Tables.Info.tables(hs, v, rows)
    } yield TildeStream(hs, v, s, tables)
  }

  val metadataRoot: P[MetadataRoot] = {
    val length = Int32
    val version = length.flatMap(l => nullTerminatedString((l + 3) / 4 * 4))
    val streamsNumber = UInt16
    val streamHeaders = streamsNumber.flatMap(l => streamHeader.rep(exactly = l))
    P(BS(0x42, 0x53, 0x4a, 0x42) ~ AnyBytes(8) ~ version ~ AnyBytes(2) ~ streamHeaders).map(MetadataRoot.tupled)
  }

  val peHeader: P[PeHeader] = {
    for {
      offset <- msDosHeader
      fileHeader <- P(AnyBytes((offset - 128).toInt) /* 2GB for .exe file should be enough*/ ~ peFileHeader)
      (sFields, ntFields, dataDirs) <- P(peHeaderStandardFields ~ ntSpecificFields ~ peHeaderDataDirectories)
      sections <- P(sectionHeader.rep(exactly = fileHeader.sectionNumber))
    } yield PeHeader(fileHeader, sFields, ntFields, dataDirs, sections)
  }

  def bytesFromRva(file: Bytes, sections: Seq[SectionHeader], rva: Long): Bytes = {
    val rvaSection = sections.find(s => rva >= s.virtualAddress && rva <= s.virtualAddress + s.virtualSize)
    rvaSection match {
      case Some(s) => {
        val start = s.pointerToRawData + rva - s.virtualAddress
        val finish = s.pointerToRawData + s.sizeOfRawData
        file.slice(start, finish) // probably should be padded with zeros to virtualSize
      }
      case None => Bytes.empty
    }
  }

  def streamHeaderBytes(file: Bytes,
                        sections: Seq[SectionHeader],
                        metadataRva: Long,
                        streamHeader: StreamHeader): Bytes = {
    val rva = metadataRva + streamHeader.offset
    bytesFromRva(file, sections, rva).take(streamHeader.size)
  }

  def parse(file: Bytes): Either[String, Pe] = {
    def toEither[T](p: Parsed[T]): Either[String, T] = p match {
      case Success(t, _)        => Right(t)
      case f @ Failure(_, _, _) => Left(f.msg)
    }

    for {
      header <- toEither(peHeader.parse(file))
      cliHeader <- toEither(
        cliHeader.parse(bytesFromRva(file, header.sectionHeaders, header.peHeaderDataDirectories.cliHeaderRva))
      )
      metadata <- toEither(
        metadataRoot.parse(bytesFromRva(file, header.sectionHeaders, cliHeader.metadataRva))
      )
      tildeHeader <- metadata.streamHeaders.find(_.name == "#~").map(Right(_)).getOrElse(Left("#~ stream not found"))
      tildeStream <- toEither(
        tildeStream.parse(streamHeaderBytes(file, header.sectionHeaders, cliHeader.metadataRva, tildeHeader))
      )
    } yield Pe(header, cliHeader, metadata, tildeStream)
  }
}
