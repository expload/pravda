package io.mytc.sood.cil

import fastparse.byte.all._
import LE._

// See
//   http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf
//   page 303
object PE {

  case class PeFileHeader(sectionNumber: Int, optionHeaderSize: Int /*probably non necessary*/, characteristics: Short)
  case class PeHeaderStandardFields(codeSize: Long,
                                    initDataSize: Long,
                                    uninitDataSize: Long,
                                    entryRva: Long,
                                    codeBase: Long,
                                    dataBase: Long)
  case class NtSpecificFields(imageBase: Long, sectionAligment: Long, imageSize: Long, headerSize: Long)
  case class PeHeaderDataDirectories(importTableBase: Long,
                                     importTableSize: Long,
                                     baseRelocationTableBase: Long,
                                     baseRelocationBaseSize: Long,
                                     importAddressTableBase: Long,
                                     importAddressTableSize: Long,
                                     cliHeaderBase: Long,
                                     cliHeaderSize: Long)
  case class SectionHeaders(name: String,
                            virtualSize: Long,
                            virtualAddress: Long,
                            sizeOfRawData: Long,
                            pointerToRawData: Long,
                            characteristics: Int)

  case class PeHeader(peFileHeader: PeFileHeader,
                      peHeaderStandardFields: PeHeaderStandardFields,
                      ntSpecificFields: NtSpecificFields,
                      peHeaderDataDirectories: PeHeaderDataDirectories,
                      sectionHeaders: Seq[SectionHeaders])

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
    val codeBase = UInt32
    val dataBase = UInt32
    P(BS(0x0B, 0x01) ~ AnyBytes(2) ~ codeSize ~ initDataSize ~ uninitDataSize ~ entryRva ~ codeBase ~ dataBase)
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
    val importTableBase = UInt32
    val importTableSize = UInt32
    val baseRelocationTableBase = UInt32
    val baseRelocationTableSize = UInt32
    val importAddressTableBase = UInt32
    val importAddressTableSize = UInt32
    val cliHeaderBase = UInt32
    val cliHeaderSize = UInt32

    P(
      AnyBytes(8) ~
        importTableBase ~ importTableSize ~
        AnyBytes(24) ~
        baseRelocationTableBase ~ baseRelocationTableSize ~
        AnyBytes(48) ~
        importAddressTableBase ~ importAddressTableSize ~
        AnyBytes(8) ~
        cliHeaderBase ~ cliHeaderSize ~
        AnyBytes(8)
    ).map(PeHeaderDataDirectories.tupled)
  }

  val sectionHeaders: P[SectionHeaders] = {
    val name = Word64.!.map(bs => {
      println(bs)
      new String(bs.takeWhile(_ != 0).toArray)
    })
    val virtualSize = UInt32
    val virtualAddress = UInt32
    val sizeOfRawData = UInt32
    val pointerToRawData = UInt32
    val characteristics = Int32

    P(name ~ virtualSize ~ virtualAddress ~ sizeOfRawData ~ pointerToRawData ~ AnyBytes(12) ~ characteristics)
      .map(SectionHeaders.tupled)
  }

  val peHeader: P[PeHeader] = {
    for {
      offset <- msDosHeader
      fileHeader <- P(AnyBytes((offset - 128).toInt)/* 2GB for .exe file should be enough*/ ~ peFileHeader)
      (sFields, ntFields, dataDirs) <- P(peHeaderStandardFields ~ ntSpecificFields ~ peHeaderDataDirectories)
      sections <- P(sectionHeaders.rep(exactly = fileHeader.sectionNumber))
    } yield PeHeader(fileHeader, sFields, ntFields, dataDirs, sections)
  }

}
