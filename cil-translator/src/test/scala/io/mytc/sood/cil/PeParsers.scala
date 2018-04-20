package io.mytc.sood.cil

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import org.scalatest.{FlatSpec, Matchers}
import utils._

import scala.collection.mutable.ArrayBuffer

class PeParsers extends FlatSpec with Matchers {
  "hello_world.exe" should "be parsed correctly" in {
    import PE.Info._
    import TablesInfo._
    import CIL._
    import TablesData._

    val helloWorldExe = Files.readAllBytes(Paths.get(this.getClass.getResource("/hello_world.exe").getPath))
    val ans = PE.parseInfo(Bytes(helloWorldExe))

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
        PeData(
          hex"""0x003c4d6f64756c653e0054657374006172677300436f6e
                736f6c650053797374656d0057726974654c696e65004f62
                6a656374002e63746f72004d61696e00746d700052756e74
                696d65436f6d7061746962696c6974794174747269627574
                650053797374656d2e52756e74696d652e436f6d70696c65
                725365727669636573006d73636f726c696200746d702e65
                7865000000""",
          hex"""0x0019480065006c006c006f00200057006f0072006c0064
                0021000000""",
          hex"""0x00040001010e03200001050001011d0e1e010001005402
                16577261704e6f6e457863657074696f6e5468726f777301
                08b77a5c561934e089""",
          Seq(0, 1, 2, 6, 8, 10, 12, 32, 35),
          Seq(
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
        ),
        Seq(
          TinyMethodHeader(hex"0x02280200000a2a"),
          TinyMethodHeader(hex"0x7201000070280100000a2a")
        )
      )
    )

    val opCodes = for {
      pe <- ans
      cilData <- CIL.fromPeData(pe.peData)
      codeParser = CIL.code(cilData)
      ops <- pe.methods.map(m => codeParser.parse(m.codeBytes).toValidated.joinRight).sequence
    } yield ops

    opCodes shouldBe Right(
      Seq(
        Seq(LdArg0, Call(Ignored), Ret),
        Seq(LdStr("Hello World!"), Call(Ignored), Ret)
      )
    )
  }
}
