package io.mytc.sood.cil

import org.scalatest.{FlatSpec, Matchers}
import java.nio.file.{Files, Paths}

import fastparse.byte.all._

import scala.collection.mutable.ArrayBuffer

class PeParsers extends FlatSpec with Matchers {
  "hello_world.exe" should "be parsed correctly" in {
    import PE._
    import Tables.Info._
    val helloWorldExe = Files.readAllBytes(Paths.get(this.getClass.getResource("/hello_world.exe").getPath))
    val ans = PE.parse(Bytes(helloWorldExe))

    helloWorldExe.length shouldBe 3072

    ans shouldBe Right(
      Pe(
        PeHeader(
          PeFileHeader(3, 224, 258),
          PeHeaderStandardFields(1024, 1536, 0, 8958, 8192, 16384),
          NtSpecificFields(4194304, 8192, 32768, 512),
          PeHeaderDataDirectories(8880, 75, 24576, 12, 8192, 8, 8200, 72),
          ArrayBuffer(
            SectionHeader(".text", 772, 8192, 1024, 512, 1610612768),
            SectionHeader(".rsrc", 720, 16384, 1024, 1536, 1073741888),
            SectionHeader(".reloc", 12, 24576, 512, 2560, 1107296320)
          )
        ),
        CliHeader(72, 2, 5, 8292, 576, 1, 100663298, 0, 0, 0, 0, 0, 0),
        MetadataRoot(
          "v4.0.30319",
          ArrayBuffer(StreamHeader(108, 220, "#~"),
                      StreamHeader(328, 148, "#Strings"),
                      StreamHeader(476, 28, "#US"),
                      StreamHeader(504, 16, "#GUID"),
                      StreamHeader(520, 56, "#Blob"))
        ),
        TildeStream(
          0,
          38654711111L,
          24190111578624L,
          List(
            ArrayBuffer(ModuleRow),
            ArrayBuffer(TypeRefRow, TypeRefRow, TypeRefRow),
            ArrayBuffer(TypeDefRow, TypeDefRow),
            ArrayBuffer(MethodDefRow(8272, 0, 6278, 52, 6, 1), MethodDefRow(8280, 0, 150, 58, 10, 1)),
            ArrayBuffer(ParamRow),
            ArrayBuffer(MemberRefRow, MemberRefRow, MemberRefRow),
            ArrayBuffer(CustomAttributeRow),
            ArrayBuffer(AssemblyRow),
            ArrayBuffer(AssemblyRefRow)
          )
        )
      ))
  }
}
